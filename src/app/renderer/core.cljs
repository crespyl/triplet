(ns app.renderer.core
  (:require
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
                    :inputs {}}))

(defn init-re-frame-effects
  "Register effect handlers for re-frame"
  []

  ;; (rf/reg-fx :update-sketch-graph
  ;;            (fn [graph]
  ;;              (sketch/update-graph-data! graph)))

  (rf/reg-fx :remove-cytoscape-triple
             (fn [triple]
               (cy/remove-triple! triple)))
  (rf/reg-fx :add-cytoscape-triple
             (fn [triple]
               (cy/add-triple! triple)))
  (rf/reg-fx :remove-cytoscape-node
             (fn [node]
               (cy/remove-node! node)))
  (rf/reg-fx :update-cytoscape-graph
             (fn [graph]
               (cy/update-cytoscape! graph)))
  (rf/reg-fx :relayout-cytoscape-graph
             (fn ([layout] (tap> layout) (cy/relayout layout)))))

(defn init-re-frame-events
  "Register event handlers for re-frame"
  []
  (rf/reg-event-fx :relayout-graph
                   (fn-traced [cofx [_ l]]
                              (let [layout (or l (-> cofx :db :inputs :layout))]
                                (tap> [:relayout-graph layout ])
                                {:relayout-cytoscape-graph layout})))

  (rf/reg-event-fx :add-statement
                   (fn-traced [cofx [_ statement]]
                              (let [new-db
                                    (update-in (:db cofx) [:graph :triple-set] #(conj % statement))]
                                {:db new-db
                                 :add-cytoscape-triple statement})))

  (rf/reg-event-fx :remove-statement
                   (fn-traced [cofx [_ statement]]
                              (let [new-db
                                    (update-in (:db cofx) [:graph :triple-set] #(disj % statement))]
                                {:db new-db
                                 :remove-cytoscape-triple statement})))

  (rf/reg-event-fx :remove-entity
                   (fn-traced [cofx [_ entity]]
                              (let [new-db
                                        ; remove entity from entity-ids list,
                                        ; then remove all statements that
                                        ; include the entity
                                    (-> (:db cofx)
                                        (update-in [:graph :entity-ids] #(disj % entity))
                                        (update-in [:graph :triple-set] (fn [triples]
                                                                          (set
                                                                           (remove #(or (= entity (first %))
                                                                                        (= entity (last %)))
                                                                                   triples)))))]
                                {:db new-db
                                 :remove-cytoscape-node entity})))

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
                              (if-let [matches (re-matches TRIPLE-REGEX input)]
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
  (init-re-frame-effects)
  (init-re-frame-events)
  (init-re-frame-subscriptions)
  (rd/render
   [ui/root-component]
   (js/document.getElementById "app-container"))
  ;(sketch/graph-view)
  (cy/init-cytoscape "cytoscape" (get-in @re-frame.db/app-db [:graph]))
  )
