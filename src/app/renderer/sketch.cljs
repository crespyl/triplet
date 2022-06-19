(ns app.renderer.sketch
  (:require
   [re-frame.core :as rf]
   [quil.core :as q :include-macros true]
   [quil.middleware :as m]
   ))

(def NODE-WIDTH 60)

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

(defn draw-node [node]
  (q/fill 50 200 50)
  (q/ellipse 0 0 NODE-WIDTH NODE-WIDTH)
  (q/fill 0)
  (q/text-align :center :center)
  (q/text-style :bold)
  (q/text (str node) 0 0))

(defn draw-sketch [state]
  (let [color (q/abs (* 255 (q/sin (/ (get-in state [:time]) 10))))
        nodes (get-in state [:graph :entity-ids])
        relations (get-in state [:graph :relation-ids])
        triples (get-in state [:graph :triple-set])
        node-text (str "Number of nodes: " (count nodes))
        rel-text  (str "Number of relations: " (count relations))]

    (q/background 255)

    (q/fill 0)
    (q/text node-text 10 10)
    (q/text rel-text 10 20)

    (doall
        (map-indexed (fn [idx node]
                       (q/push-matrix)
                       (q/translate (* (+ idx 1) (+ NODE-WIDTH 5)) (/ (q/width) 2))
                       (draw-node node)
                       (q/pop-matrix)
                       )
                     nodes))))

(q/defsketch graph-view
  :features [:no-start]
  :middleware [m/fun-mode]
  :setup setup-sketch
  :update update-sketch
  :draw draw-sketch
  :host "sketch"
  :size [300 300])
