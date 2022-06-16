(ns app.renderer.core
  (:require
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [reagent.core :refer [atom]]
   [reagent.dom :as rd]
   [re-frame.core :as rf]))

(enable-console-print!)

(def TRIPLE-REGEX #"([\w:-]+)\s+([\w:-]+)\s+([\w:-]+)")

;; re-frame init app state
(rf/reg-event-db :initialize
                 (fn [_ _]
                   {:time (js/Date.)
                    :triple-set #{}
                    :entity-names (sorted-set)
                    :relation-names (sorted-set)}))

;;;;;;;;;;;;;;;;;;;;;
;; re-frame event handlers

(rf/reg-event-db :timer
                 (fn [db [_ new-time]]
                   (assoc db :time new-time)))

(rf/reg-event-db :add-statement
                 (fn-traced [db [_ statement]]
                   (let [])
                   (update-in db [:triple-set] #(conj % statement))))

(rf/reg-event-db :add-entity-name
                 (fn-traced [db [_ entity-name]]
                   (let [])
                   (update-in db [:entity-names] #(conj % entity-name))))

(rf/reg-event-db :add-relation-name
                 (fn-traced [db [_ relation-name]]
                   (let [])
                   (update-in db [:relation-names] #(conj % relation-name))))

(rf/reg-event-db :process-input
                 (fn-traced [db [_ input]]
                   (if-let [matches (re-matches TRIPLE-REGEX input)]
                     (let [valid-triple (rest matches)]
                       (do
                         (rf/dispatch [:add-statement     valid-triple])
                         (rf/dispatch [:add-entity-name   (first valid-triple)])
                         (rf/dispatch [:add-relation-name (second valid-triple)])
                         (rf/dispatch [:add-entity-name   (last valid-triple)])))
                     (.log js/console (str "ignoring invalid statement: " input)))))

;; re-frame subscriptions
(rf/reg-sub :time
            (fn [db _]
              (:time db)))

(rf/reg-sub :triple-set
            (fn [db _]
              (:triple-set db)))

(rf/reg-sub :entity-names
            (fn [db _]
              (:entity-names db)))

(rf/reg-sub :relation-names
            (fn [db _]
              (:relation-names db)))

(defn dispatch-timer-event []
  (let [now (js/Date.)]
    (rf/dispatch [:timer now])))
;;(defonce do-timer (js/setInterval dispatch-timer-event 1000))

;;;;;;;;;;;;;;;;;;;;;
;; components

(defn clock []
  (let [time (-> @(rf/subscribe [:time])
                 .toTimeString
                 (clojure.string/split " ")
                 first)]
    [:div.clock time]))

(defn input-form []
  "Builds the basic input form with text field and submit button"
  (let [gettext (fn [e] (-> e .-target .-elements first .-value))]
    [:form {:on-submit (fn [e]
                         (.preventDefault e)
                         (rf/dispatch [:process-input (gettext e)]))}
     [:input#main-input {:type "text"}]
     [:button {:type "submit"}
      (str "Parse")]]))

(defn triple [subject predicate object]
  [:div.triple
   [:span.entity.subject subject]
   [:span.predicate predicate]
   [:span.entity.object object]])

(defn triple-log []
  [:ol#triple-log
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:triple-set])]
     ^{:key entry}
     [:li [apply triple entry]])])

(defn entity-names []
  [:ul#entity-names
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:entity-names])]
     ^{:key (str "entity-" entry)}
     [:li.entity entry])])

(defn relation-names []
  [:ul#relation-names
   {:style {:display "inline-block"}}
   (for [entry @(rf/subscribe [:relation-names])]
     ^{:key (str "relation-" entry)}
     [:li.predicate entry])])

(defn root-component []
  [:div
   [:div.logos
    [:img.electron {:src "img/electron-logo.png"}]
    [:img.cljs {:src "img/cljs-logo.svg"}]
    [:img.reagent {:src "img/reagent-logo.png"}]]
   [clock]
   [:div#graph
    [:p (str "Graph canvas goes here")]]
   [:div#controls [input-form]]
   [triple-log]
   [entity-names]
   [relation-names]
   ])

(defonce initialized?
  (rf/dispatch-sync [:initialize]))

(defn ^:dev/after-load start! []
  (rd/render
   [root-component]
   (js/document.getElementById "app-container")))
