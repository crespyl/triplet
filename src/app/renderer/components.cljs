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
        "Re-layout"]
       [:label {:for :auto-relayout} "Auto Re-layout"]
       [:input {:field :checkbox :id :auto-relayout :checked true}]]]
     reagent-forms-events]))

(defn entity [id]
  [:span.entity id])

(defn relation [id]
  [:span.relation id])

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

(defn sidebar []
  [:div#sidebar])


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
