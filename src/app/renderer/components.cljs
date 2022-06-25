(ns app.renderer.components
  (:require
   [re-frame.core :as rf]
   [reagent-forms.core :refer [bind-fields]]))

(def reagent-forms-events
  {:get     (fn [path]
              @(rf/subscribe [:value path]))
   :save!   (fn [path value]
              (rf/dispatch [:set-value path value]))
   :update! (fn [path save-fn value]
              ; save-fn should accept two arguments: old-value, new-value
              (rf/dispatch [:update-value save-fn path value]))
   :doc     (fn [] @(rf/subscribe [:inputs]))})

(defn graph-view [host]
  [:div {:id host :class ["sketch quill"]}])

(defn cytoscape-view [container]
  [:div {:id container
         :class ["cytoscape"]
         }])

(defn input-form []
  (let [gettext (fn [e] (-> e .-target .-elements first .-value))]
    [bind-fields
     [:form.grid {:on-submit (fn [e]
                          (.preventDefault e)
                          (rf/dispatch [:process-input (gettext e)]))}
      [:div.row
       [:label {:for :main-input} "Enter triple:"]
       [:input {:field :text
                :id :main-input
                :placeholder "subject predicate object"}]
       [:button {:type :submit}
        "Parse"]
       ]
      [:div.row
       [:label {:for :layout} "Layout:"]
       [:select {:field :list :id :layout :defaultValue :cose-bilkent}
        (for [l [:fcose :cose-bilkent :breadthfirst :random :grid :circle]]
          [:option {:key l} (subs (str l) 1)])]
       [:button {:on-click (fn [e]
                             (.preventDefault e)
                             (rf/dispatch [:relayout-graph]))}
        "Run Layout"]
       [:label {:for :auto-relayout} "Auto Re-layout"]
       [:input {:field :checkbox :id :auto-relayout :checked false}]]]
     reagent-forms-events]))

(defn entity [id]
  (let [meta @(rf/subscribe [:entity-meta id])
        label (if-not (empty? (:label meta))
                (:label meta)
                id)]
    [:span.entity label]))

(defn relation [id]
  (let [meta @(rf/subscribe [:entity-meta id])
        label (if-not (empty? (:label meta))
                (:label meta)
                id)]
    [:span.relation label]))

(defn triple [subject predicate object]
  [:div.triple-container
   [:div.triple
    [entity subject]
    [relation predicate]
    [entity object]]
   [:button.delete-button
    {:on-click #(rf/dispatch [:remove-statement [subject predicate object]])}
    "X"]])

(defn triple-log []
  [:ol#triple-log
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:triple-set])]
     ^{:key (hash entry)}
     [:li [apply triple entry]])])

(defn entity-ids []
  [:ul#entity-ids
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:entity-ids])]
     ^{:key (str "entity-" entry)}
     [:li [entity entry]])])

(defn relation-ids []
  [:ul#relation-ids
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:relation-ids])]
     ^{:key (str "relation-" entry)}
     [:li [relation entry]])])

(defn entity-meta [entity-id]
  (let [meta @(rf/subscribe [:entity-meta entity-id])]
    ^{:key (str "entity-meta-box-" entity-id)}
    [:div.entity-meta-box
     {:data-entity-id entity-id}
     [:div.meta-row.entity-id
      [:label "ID:"]
      [:span entity-id]]
     (let [supported-keys [:label :width :height]]
       (for [key supported-keys]
         ^{:key (str "entity-meta-box-row" entity-id key)}
         [:div.meta-row
          [:label (str (subs (str key) 1) ": ")]
          [:input {:value (key meta)
                   :on-change (fn [evt]
                                (let [value (-> evt .-target .-value)
                                      new-meta (assoc-in meta [key] value)]
                                  (tap> [:on-change entity-id key value meta new-meta ])

                                  (rf/dispatch [:set-entity-meta entity-id
                                                (if-not (empty? value)
                                                  (assoc-in meta [key] value)
                                                  (dissoc meta [key]))])))}]
          ]))])
  ;[:div (str entity-id)]
  )

(defn sidebar []
  [:div#sidebar
   [:div "Selection:"]
   (let [selection @(rf/subscribe [:selection])]
     (if-not (empty? selection)
      (for [entity-id selection]
        ^{:key (str "sidebar-meta-" entity-id)}
        [entity-meta entity-id])
      [:span "Nothing selected"]))])

(defn root-component []
  [:<>
   ;[graph-view "sketch"]
   [cytoscape-view "cytoscape"]
   [sidebar]
   [:div#controls [input-form]]
   [:div#info.grid
    [:div.scroll-list [triple-log]]
    [:div.scroll-list [entity-ids]]
    [:div.scroll-list [relation-ids]]]
   ])
