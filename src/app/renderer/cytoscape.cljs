(ns app.renderer.cytoscape
  (:require
   ["cytoscape" :as cytoscape]
   ["cytoscape-cola" :as cola]))

(def cy nil)

(defn graph-to-cytoscape [graph]
  "converts a list of entity ids/names and a set of triples to an object with
  cytoscape-ready elements and edges"
  (let [nodes (map (fn [entity-id]
                     { :group "nodes"
                      :data { :id entity-id :weight 1 } })
                   (:entity-ids graph))
        edges (map-indexed (fn [idx [subj pred obj]]
                             { :group "edges"
                              :data {
                                     :id (hash [subj pred obj])
                                     :label pred
                                     :source subj
                                     :target obj
                                     :directed true
                                     }})
                           (:triple-set graph))]
    {
     :nodes nodes
     :edges edges
     }))

(defn update-cytoscape [graph]
  (let [cy-graph (graph-to-cytoscape graph)
        nodes (:nodes cy-graph)
        edges (:edges cy-graph)]
    (.add cy (clj->js nodes))
    (.add cy (clj->js edges))
    (.run (.layout cy (clj->js {
                                :name "cola"
                                :animate true
                                :refresh 20
                                }))))
  )

(defn init-cytoscape [container graph]
  (let [el (js/document.getElementById container)]
    (set! cy (cytoscape (clj->js  {
                                   :container el
                                   :style [
                                           {
                                            :selector "node"
                                            :style {
                                                    :label "data(id)"
                                                    :text-valign "center"
                                                    :color "#000000"
                                                    :background-color "#3a7ecf"
                                                    }}
                                           {
                                            :selector "edge"
                                            :style {
                                                    :width 2
                                                    :line-color "black"
                                                    :opacity "0.5"
                                                    :label "data(label)"
                                                    :mid-target-arrow-shape "triangle"
                                                    :mid-target-arrow-color "#000000"
                                                    }
                                            }
                                           {
                                            :selector "edge[label]"
                                            :css {
                                                  :label "data(label)"
                                                  :text-rotation "autorotate"
                                                  :text-margin-x "0px"
                                                  :text-margin-y "10px"
                                                  :text-valign "top"
                                                  :text-halign "center"
                                                  }
                                            }
                                           ]
                                   })))
    (.use cytoscape (clj->js cola))
    (update-cytoscape graph))
  )
