(ns app.renderer.core
  (:require

   ;; cljs
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [reagent.core :as r]
   [reagent.dom :as rd]
   [re-frame.core :as rf]
   [reagent-forms.core :refer [bind-fields]]
   [dorothy.core :as dot]
   ))

(enable-console-print!)

(def TRIPLE-REGEX #"([\w*/:-]+)\s+([\w*/:-]+)\s+([\w*/:-]+)")


;; re-frame init app state
(rf/reg-event-db :initialize
                 (fn [_ _]
                   {:graph {:triple-set   (sorted-set)
                            :entity-ids   (sorted-set)
                            :relation-ids (sorted-set)}
                    :inputs {}}))

;;;;;;;;;;;;;;;;;;;;;
;; re-frame event handlers
(rf/reg-event-db :add-statement
                 (fn-traced [db [_ statement]]
                   (update-in db [:graph :triple-set] #(conj % statement))))

(rf/reg-event-db :add-entity-name
                 (fn-traced [db [_ entity-name]]
                   (update-in db [:graph :entity-ids] #(conj % entity-name))))

(rf/reg-event-db :add-relation-name
                 (fn-traced [db [_ relation-name]]
                   (update-in db [:graph :relation-ids] #(conj % relation-name))))

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
              (get-in doc path)))
(rf/reg-event-db :set-value
                 (fn [db [_ path value]]
                   (assoc-in db (into [:inputs] path) value)))

(rf/reg-event-db :update-value
                 (fn [db [_ f path value]]
                   (update-in db (into [:inputs] path) f value)))
(def reagent-forms-events
  {:get (fn [path] @(rf/subscribe [:value path]))
   :save! (fn [path value] (rf/dispatch [:set-value path value]))
   :update! (fn [path save-fn value]
              ; save-fn should accept two arguments: old-value, new-value
              (rf/dispatch [:update-value save-fn path value]))
   :doc (fn [] @(rf/subscribe [:inputs]))})

;; reagent-forms events -> re-frame

;;;;;;;;;;;;;;;;;;;;;
;; components

(defn ac-source [input]
  (let [full-list (concat
                   @(rf/subscribe [:graph :entity-ids])
                   @(rf/subscribe [:graph :relation-ids]))]
    (if (= input :all)
      full-list
      (filter
       #(-> % (.toLowerCase %) (.indexOf input) (> -1))
       (conj full-list input)))))

;; (defn old-test-autocopmlete []
;;   (let [dataProvider (fn [token]
;;                        (tap> [:dataProvider token])
;;                        (clj->js ["foo" "bar" "baz"]))
;;         loading-component (r/reactify-component ac-loader)
;;         suggestion-component (r/reactify-component ac-suggestion)
;;         trigger-parameters {:dataProvider dataProvider
;;                             :component suggestion-component
;;                             :output #(str %1)}
;;         entities (vec @(rf/subscribe [:entity-ids]))
;;         relations (vec @(rf/subscribe [:relation-ids]))
;;         triggers (map #(clojure.string/join (take 1 %))
;;                       (concat entities relations))
;;         trigger-map (reduce #(conj %1 %2 trigger-parameters) [] triggers)
;;         dummy (tap> [:triggers triggers])
;;         dummy (tap> [:trigger-map trigger-map])
;;         dummy (tap> [:map (apply hash-map trigger-map)])
;;         ;trigger-map (reduce #(conj %1 %2 trigger-parameters) [] triggers)
;;         ]

;;     [:div {:style {:width 500}}
;;      [react-textarea-autocomplete
;;       {
;;        :className "my-textarea"
;;        :textAreaComponent "input"
;;        :loadingComponent loading-component
;;        :trigger (apply hash-map trigger-map)
;;        :onCaretPositionChange #(tap> [:ocpc %])
;;        }]]))

;; (defn test-autocomplete [id placeholder]
;;   [bind-fields
;;    [:div.typeahead {:field             :typeahead
;;                     :id                id
;;                     :input-placeholder placeholder
;;                     :data-source       ac-source
;;                     :input-class       "form-control"
;;                     :list-class        "typeahead-list"
;;                     :item-class        "typeahead-item"
;;                     :highlight-class   "typeahead-highlighted"
;;                     :get-index         (constantly 0)}]
;;    reagent-forms-events])

;; (defn autocomplete-input-form []
;;   [:form
;;    [test-autocomplete :form.entity    "entity"]
;;    [test-autocomplete :form.attribute "attribute"]
;;    [test-autocomplete :form.value     "value"]])

(defn graph-view []
 [:span "Graph goes here"]
  )

(defn input-form []
  (let [gettext (fn [e] (-> e .-target .-elements first .-value))]
    [bind-fields
     [:form {:on-submit (fn [e]
                          (.preventDefault e)
                          (rf/dispatch [:process-input (gettext e)]))}
      [:input {:field :text :id :main-input}]
      [:button {:type :submit}
       "Parse"]]
     reagent-forms-events]))

(defn triple [subject predicate object]
  [:div.triple
   [:span.entity.subject subject]
   [:span.predicate    predicate]
   [:span.entity.object   object]])

(defn triple-log []
  [:ol#triple-log
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:triple-set])]
     ^{:key entry}
     [:li [apply triple entry]])])

(defn entity-ids []
  [:ul#entity-ids
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:entity-ids])]
     ^{:key (str "entity-" entry)}
     [:li.entity entry])])

(defn relation-ids []
  [:ul#relation-ids
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:relation-ids])]
     ^{:key (str "relation-" entry)}
     [:li.predicate entry])])

(defn root-component []
  [:div
   [:div.logos
    [:img.electron {:src "img/electron-logo.png"}]
    [:img.cljs {:src "img/cljs-logo.svg"}]
    [:img.reagent {:src "img/reagent-logo.png"}]]
   [:div#graph
    [graph-view]]
   [:div#controls [input-form]]
   ;;[:div#new-controls [autocomplete-input-form]]
   [triple-log]
   [entity-ids]
   [relation-ids]
   ])

(defonce initialized?
  (rf/dispatch-sync [:initialize]))

(defn ^:dev/after-load start! []
  (rd/render
   [root-component]
   (js/document.getElementById "app-container")))
