(ns app.renderer.core
  (:require
   [clojure.string :as string]
   [app.renderer.components :as ui]
   [app.renderer.cytoscape :as cy]
   [app.renderer.loader :as loader]
   ;[app.renderer.sketch :as sketch]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [re-frame.core :as rf]
   [re-frame.db]
   [reagent.dom :as rd]))

(enable-console-print!)

(def TRIPLE-REGEX #"([_()\w*/:-]+)\s+([_()\w*/:-]+)\s+([_()\w*/:-]+)")

;; re-frame init app state
(rf/reg-event-db :initialize
                 (fn [_ _]
                   {:graph {:triple-set   (sorted-set)
                            :entity-ids   (sorted-set)
                            :relation-ids (sorted-set)}
                    :cytoscape nil
                    :automove-predicate-nodes true
                    :inputs {}}))

(defn init-re-frame-events
  "Register event handlers for re-frame"
  []

  (rf/reg-event-db :init-cytoscape
                   (fn-traced [db [_ container]]
                              (let [first-init (not (:cytoscape db))
                                    cytoscape (if first-init
                                                (cy/init-cytoscape container (:graph db))
                                                (:cytoscape db))]
                                (if first-init
                                  (do
                                    (tap>  [:init])
                                    (cy/register-extensions! cytoscape)
                                    (cy/enable-automove! cytoscape)
                                    (cy/setup-lock-toggle! cytoscape))
                                  (do
                                    (tap> [:remount])
                                    (cy/remount cytoscape container)))
                                (assoc-in db [:cytoscape] cytoscape))))
  (rf/reg-event-db :set-automove-predicate-nodes
                   (fn-traced [db [_ enable-automove?]]
                              (if enable-automove?
                                (-> db
                                    (update-in [:cytoscape] #(cy/enable-automove! %))
                                    (assoc-in [:automove-predicate-nodes] true))
                                (-> db
                                    (update-in [:cytoscape] #(cy/disable-automove! %))
                                    (assoc-in [:automove-predicate-nodes] false)))))

  (rf/reg-event-db :relayout-graph
                   (fn-traced [db [_ l]]
                     (let [layout (or l (get-in db [:inputs :layout]))]
                       (-> db
                           (assoc-in [:inputs :layout] layout)
                           (update-in [:cytoscape] #(cy/relayout (:cytoscape db) layout))))))

  (rf/reg-event-db :add-statement
                   (fn-traced [db [_ statement skip-relayout]]
                              (tap> [:add-statement statement skip-relayout])
                              (when (and (nil? skip-relayout)
                                         (get-in db [:inputs :auto-relayout]))
                                (rf/dispatch [:relayout-graph]))
                              (-> db
                                  (update-in [:graph :triple-set] #(conj % statement))
                                  (update-in [:cytoscape] #(cy/add-triple! % statement))
                                  (update-in [:cytoscape] #(cy/re-apply-automove! %)))))

  (rf/reg-event-db :remove-statement
                   (fn-traced [db [_ statement]]
                     (-> db
                         (update-in [:graph :triple-set] #(disj % statement))
                         (update-in [:cytoscape] #(cy/remove-triple! % statement)))))

  (rf/reg-event-db :remove-entity
                   (fn-traced [db [_ entity]]
                     (-> db
                         (update-in [:graph :entity-ids] #(disj % entity))
                         (update-in [:graph :triple-set] (fn [triples]
                                                           (set
                                                            (remove #(or (= entity (first %))
                                                                         (= entity (last %)))
                                                                    triples))))
                         (update-in [:cytoscape] #(cy/remove-node! % entity)))))

  (rf/reg-event-fx :add-entity-name
                   (fn-traced [cofx [_ ent]]
                     (let [new-db
                           (update-in (:db cofx) [:graph :entity-ids] #(conj % ent))]
                       {:db new-db
                        ;:update-cytoscape-graph (get-in new-db [:graph])
                        })))

  (rf/reg-event-fx :add-relation-name
                   (fn-traced [cofx [_ rel]]
                              (let [new-db
                                    (update-in (:db cofx) [:graph :relation-ids] #(conj % rel))]
                                {:db new-db
                                 ;:update-cytoscape-graph (get-in new-db [:graph])
                                 })))

  (rf/reg-event-db :process-input
                   (fn-traced [db [_ input]]
                              (if-let [matches (re-matches TRIPLE-REGEX (string/trim input))]
                                (let [[sub pred obj] (rest matches)]
                                  (if-not (contains? (-> db :graph :triple-set) [sub pred obj])
                                    (do
                                      (if-not (contains? (-> db :graph :entity-ids)   sub)  (rf/dispatch [:add-entity-name   sub]))
                                      (if-not (contains? (-> db :graph :relation-ids) pred) (rf/dispatch [:add-relation-name pred]))
                                      (if-not (contains? (-> db :graph :entity-ids)   obj)  (rf/dispatch [:add-entity-name   obj]))))
                                      (rf/dispatch [:add-statement [sub pred obj]])
                                  (rf/dispatch [:set-value [:main-input] ""]))
                                (.log js/console (str "ignoring invalid statement: " input)))))

  ;; general purpose setters for use with reagent-forms
  (rf/reg-event-db :set-value
                   (fn [db [_ path value]]
                     (assoc-in db (into [:inputs] path) value)))

  (rf/reg-event-db :update-value
                   (fn [db [_ f path value]]
                     (update-in db (into [:inputs] path) f value))))

(defn init-re-frame-subscriptions
  "Register subscribtions/queries for re-frame"
  []
  (rf/reg-sub :triple-set
              (fn [db _]
                (-> db :graph :triple-set)))

  (rf/reg-sub :entity-ids
              (fn [db _]
                (-> db :graph :entity-ids)))

  (rf/reg-sub :relation-ids
              (fn [db _]
                (-> db :graph :relation-ids)))

  (rf/reg-sub :selected-layout
              (fn [db _]
                (-> db :inputs :layout)))

  ;; reagent-forms integration hooks
  (rf/reg-sub :inputs
              (fn [db _]
                (:inputs db)))
  (rf/reg-sub :value
              :<- [:inputs]
              (fn [doc [_ path]]
                (get-in doc path))))

(defonce initialized?
  (do
    (rf/dispatch-sync [:initialize])
    true))

(defn ^:dev/after-load start! []
  (init-re-frame-events)
  (init-re-frame-subscriptions)
  (rd/render
   [ui/root-component]
   (js/document.getElementById "app-container"))

  (rf/dispatch-sync [:init-cytoscape "cytoscape"]))

;(def rows (app.renderer.loader/simple-parse app.renderer.loader/demo-csv))
;(doseq [r (drop 1 rows)] (rf/dispatch [:add-statement r true]))
