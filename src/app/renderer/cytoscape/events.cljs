(ns app.renderer.cytoscape.events
  (:require [re-frame.core :as rf]))

(defn setup-lock-toggle! [cy]
  (.on cy "dblclick" "node" (fn [evt]
                              (let [node (.-target evt)]
                                (if (.locked node)
                                  (.unlock node)
                                  (.lock node))))))

(defn setup-select-node! [cy]
  (.on cy "select" "node" (fn [evt]
                            (let [node (.-target evt)]
                              (rf/dispatch [:select-entity (.id node)])))))

(defn setup-deselect-node! [cy]
  (.on cy "tapunselect" "node" (fn [evt]
                            (let [node (.-target evt)]
                              (rf/dispatch [:deselect-entity (.id node)])))))

(defn setup-dblclick-canvas! [cy]
  (.on cy "dblclick" (fn [evt]
                       (when (= cy (.-target evt))
                         (rf/dispatch
                          [:dblclick-canvas
                           (js->clj {:position (.-position evt)
                                     :rendered-position (.-renderedPosition evt)})])))))

(defn clear-event-handlers! [cy]
  (.off cy "tap"))

(defn setup-default-events [cy]
  (setup-lock-toggle! cy)
  (setup-select-node! cy)
  (setup-deselect-node! cy)
  (setup-dblclick-canvas! cy))
