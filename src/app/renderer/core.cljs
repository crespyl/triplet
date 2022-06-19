(ns app.renderer.core
  (:require
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [reagent.core :as r]
   [reagent.dom :as rd]
   [re-frame.core :as rf]
   [reagent-forms.core :refer [bind-fields]]
   [quil.core :as q]

   [app.renderer.components :as ui]

   [app.renderer.sketch :as sketch]))

(enable-console-print!)

(def TRIPLE-REGEX #"([\w*/:-]+)\s+([\w*/:-]+)\s+([\w*/:-]+)")

;; re-frame init app state
(rf/reg-event-db :initialize
                 (fn [_ _]
                   {:graph {:triple-set   (sorted-set)
                            :entity-ids   (sorted-set)
                            :relation-ids (sorted-set)}
                    :sketch {}
                    :inputs {}}))

(defn init-re-frame-effects []
  (rf/reg-fx :update-sketch-graph
             (fn [graph]
               (sketch/update-graph-data! graph))))

(defn init-re-frame-events []
  "Register event handlers for re-frame"
  (rf/reg-event-db :add-statement
                   (fn-traced [db [_ statement]]
                              (update-in db [:graph :triple-set] #(conj % statement))))

  (rf/reg-event-fx :add-entity-name
                   (fn-traced [cofx [_ ent]]
                     (let [new-db
                           (update-in (:db cofx) [:graph :entity-ids] #(conj % ent))]
                       {:db new-db
                        :update-sketch-graph (get-in new-db [:graph])})))

  (rf/reg-event-fx :add-relation-name
                   (fn-traced [cofx [_ rel]]
                              (let [new-db
                                    (update-in (:db cofx) [:graph :relation-ids] #(conj % rel))]
                                {:db new-db
                                 :update-sketch-graph (get-in new-db [:graph])})))

  (rf/reg-event-db :process-input
                   (fn-traced [db [_ input]]
                              (if-let [matches (re-matches TRIPLE-REGEX input)]
                                (let [[sub pred obj] (rest matches)]
                                  (do
                                    (rf/dispatch [:add-statement [sub pred obj]])
                                    (if-not (contains? (-> db :graph :entity-ids)   sub)  (rf/dispatch [:add-entity-name   sub]))
                                    (if-not (contains? (-> db :graph :relation-ids) pred) (rf/dispatch [:add-relation-name pred]))
                                    (if-not (contains? (-> db :graph :entity-ids)   obj)  (rf/dispatch [:add-entity-name   obj]))
                                    (rf/dispatch [:set-value [:main-input] ""])))
                                (.log js/console (str "ignoring invalid statement: " input)))))

  ;; general purpose setters for use with reagent-forms
  (rf/reg-event-db :set-value
                   (fn [db [_ path value]]
                     (assoc-in db (into [:inputs] path) value)))

  (rf/reg-event-db :update-value
                   (fn [db [_ f path value]]
                     (update-in db (into [:inputs] path) f value))))

(defn init-re-frame-subscriptions []
  "Register subscribtions/queries for re-frame"
  ;; re-frame subscriptions
  (rf/reg-sub :triple-set
              (fn [db _]
                (-> db :graph :triple-set)))

  (rf/reg-sub :entity-ids
              (fn [db _]
                (-> db :graph :entity-ids)))

  (rf/reg-sub :relation-ids
              (fn [db _]
                (-> db :graph :relation-ids)))

  ;; reagent-forms integration hooks
  (rf/reg-sub :inputs
              (fn [db _]
                (:inputs db)))
  (rf/reg-sub :value
              :<- [:inputs]
              (fn [doc [_ path]]
                (get-in doc path))))

(defn root-component []
  [:div
   [:div.logos
    [:img.electron {:src "img/electron-logo.png"}]
    [:img.cljs {:src "img/cljs-logo.svg"}]
    [:img.reagent {:src "img/reagent-logo.png"}]]
   [ui/graph-view "sketch"]
   [:div#controls [ui/input-form]]
   [ui/triple-log]
   [ui/entity-ids]
   [ui/relation-ids]
   ])

(defonce initialized?
  (do
    (rf/dispatch-sync [:initialize])
    true))

(defn ^:dev/after-load start! []
  (init-re-frame-effects)
  (init-re-frame-events)
  (init-re-frame-subscriptions)
  (tap> [:after-load])
  (rd/render
   [root-component]
   (js/document.getElementById "app-container"))
  (sketch/graph-view))
