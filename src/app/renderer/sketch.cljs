(ns app.renderer.sketch
  (:require
   [re-frame.core :as rf]
   [quil.core :as q :include-macros true]
   [quil.middleware :as m]
   ))

(def ext-state (atom {}))
(defn update-graph-data! [new-data]
  (swap! ext-state
         (fn [old-state]
           (-> old-state
               (assoc-in [:graph] new-data)))))

(defn setup-sketch []
  (q/frame-rate 10)
  {:time 0
   :graph {}})

(defn update-sketch [state]
  (-> state
      (assoc-in [:graph] (get-in @ext-state [:graph]))
      (update-in [:time] inc)))

(defn draw-sketch [state]
  (let [color (q/abs (* 255 (q/sin (/ (get-in state [:time]) 50))))
        node-text (str "Number of nodes: " (count (get-in state [:graph :entity-ids])))
        rel-text  (str "Number of relations: " (count (get-in state [:graph :relation-ids])))]
    (q/background 255)
    (q/fill color 255 (- 255 color))
    (q/ellipse (/ (q/width) 2) (/ (q/height) 2) 55 55)
    (q/fill 0)
    (q/text node-text 10 10)
    (q/text rel-text 10 20)))

(q/defsketch graph-view
  :features [:no-start]
  :middleware [m/fun-mode]
  :setup setup-sketch
  :update update-sketch
  :draw draw-sketch
  :host "sketch"
  :size [300 300])
