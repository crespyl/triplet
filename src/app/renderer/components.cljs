(ns app.renderer.components
  (:require
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [reagent.core :as r]
   [reagent.dom :as rd]
   [re-frame.core :as rf]
   [reagent-forms.core :refer [bind-fields]]

   [app.renderer.sketch :as sketch]
   ))

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
         :style {
                 :width "50%"
                 :height 300
                 }}])

(defn input-form []
  (let [gettext (fn [e] (-> e .-target .-elements first .-value))]
    [bind-fields
     [:form {:on-submit (fn [e]
                          (.preventDefault e)
                          (rf/dispatch [:process-input (gettext e)]))}
      [:div.row
       [:label {:for :main-input} "Enter triple:"]
       [:input {:field :text
                :id :main-input
                :placeholder "subject predicate object"}]
       [:button {:type :submit}
        "Parse"]]
      [:div.row
       [:label {:for :layout} "Layout:"]
       [:select {:field :list :id :layout}
        (for [l [:breadthfirst :random :grid :circle :cose :cose-bilkent]]
          [:option {:key l} (subs (str l) 1)])]
       [:button {:on-click (fn [e]
                             (.preventDefault e)
                             (tap> [:relayout-onclick])
                             (rf/dispatch [:relayout-graph @(rf/subscribe [:selected-layout])]))}
        "Re-layout"]]]
     reagent-forms-events]))

(defn triple [subject predicate object]
  [:div.triple
   [:span.entity.subject subject]
   [:span.predicate    predicate]
   [:span.entity.object   object]])

(defn triple-log []
  [:ol#triple-log.scroll-list
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:triple-set])]
     ^{:key (hash entry)}
     [:li [apply triple entry]])])

(defn entity-ids []
  [:ul#entity-ids.scroll-list
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:entity-ids])]
     ^{:key (str "entity-" entry)}
     [:li.entity entry])])

(defn relation-ids []
  [:ul#relation-ids.scroll-list
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:relation-ids])]
     ^{:key (str "relation-" entry)}
     [:li.predicate entry])])
