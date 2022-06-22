(ns app.renderer.cytoscape
  (:require
   [clojure.string :as string]
   ["cytoscape" :as cytoscape]
   ["cytoscape-cola" :as cola]
   ["cytoscape-cose-bilkent" :as cose-bilkent]))

(def cy nil)
(def layout-name "breadthfirst")

(defn node-to-cytoscape-node
  "Transforms node-id into a hash with additional data for use in Cytoscape,
  with optional additional metadata passed in the second parameter"
  ([node-id]
   (node-to-cytoscape-node node-id nil))
  ([node-id meta]
   (let [data (merge {:id node-id
                      :weight 1
                      :shape "round-rectangle"
                      :width 60
                      :height 60
                      :name node-id ;(string/capitalize node-id)
                      }
                     meta)]
     {:group "nodes"
      :data data})))

(defn triple-to-cytoscape-edge
  "Transforms a triple of [node-id rel-id node-id] into a single Cytoscape
  element representing just that edge (i.e. not including the nodes)"
  [[subj pred obj]]
  (identity {:group "edges"
             :data {
                    :id (hash [subj pred obj])
                    :label pred
                    :source subj
                    :target obj
                    :directed true
                    :edge-type pred
                    }}))

(defn triple-to-cytoscape-node
  "Transforms a triple of [node-id rel-id node-id] into a single Cytoscape
  element representing just that edge (i.e. not including the nodes)"
  [[subj pred _obj]]
  (tap> [:to-node subj pred _obj])
  (identity {:group "nodes"
             :data {
                    :id (hash [subj pred])
                    :label pred
                    :triplet-type :predicate-cluster
                    :shape "circle"
                    }}))

(defn make-cytoscape-edge [src tgt]
  (identity {:group "edges"
             :data {
                    :id (hash [src tgt])
                    :source (-> src :data :id)
                    :target (-> tgt :data :id)
                    :directed true
                    }}))

(defn triple-to-cytoscape-elements
  "Transforms a triple of [node-id rel-id node-id] into a vec of hashes
  representing Cytoscape elements, reifying the relation into [[node -> rel],
  [rel -> node]] pairs"
  [[subj pred obj]]
  (let [subj-ele (node-to-cytoscape-node subj)
        obj-ele  (node-to-cytoscape-node obj)
        pred-ele (triple-to-cytoscape-node [subj pred obj])]
    [subj-ele (make-cytoscape-edge subj-ele pred-ele)
     pred-ele (make-cytoscape-edge pred-ele obj-ele)
     obj-ele]
    ))

(defn relayout
  "Triggers the Cytoscape view to re-apply the selected layout"
  ([] (relayout layout-name))
  ([name] (let [layout-params (clj->js {
                                       :name name
                                       :animate true
                                       :refresh 20
                                       })]
            (set! layout-name name)
            (.run (.layout cy layout-params))
            )))

(defn add-triple! [triple]
  (let [eles (triple-to-cytoscape-elements triple)]
    (tap> [:add-triple eles])
    (.add cy (clj->js eles))
    ;(relayout layout-name)
    ))

(defn graph-to-cytoscape
  "converts a list of entity ids/names and a set of triples to an object with
  cytoscape-ready elements and edges"
  [graph]
  (let [nodes (map node-to-cytoscape-node (:entity-ids graph))
        edges (map triple-to-cytoscape-edge (:triple-set graph))]
    {:nodes nodes
     :edges edges}))

(defn remove-triple!
  "Remove a triple from the Cytoscape graph"
  ([triple]
   (let [pairs [[(first triple) (second triple)]
                [(second triple) (last triple)]]
         ids (map hash pairs)]
     (tap> [:remove-triple pairs ids])
     (if-not (empty? ids)
       (do
         (doseq [id ids]
           (.remove cy (.getElementById cy id)))
         true)
       false))))

(defn remove-node!
  "Remove a node from the Cytoscape graph"
  ([node]
   (let [ele (.getElementById cy node)]
     (if-not (empty? ele)
       (do
         (.remove cy ele)
         true)
       false))))

(defn update-cytoscape! [graph]
  (let [cy-graph (graph-to-cytoscape graph)
        nodes (clj->js (flatten (:nodes cy-graph)))
        edges (clj->js (flatten (:edges cy-graph)))]
    (.add cy nodes)
    (.add cy edges)
    (relayout layout-name)
    ))

(defn init-cytoscape [container graph]
  (let [el (js/document.getElementById container)]
    (tap> [:init-cytoscape])
    (set! cy (cytoscape (clj->js  {
                                   :container el
                                   :style [
                                           {
                                            :selector "node"
                                            :style {
                                                    :label (fn [ele]  ;"data(id)"
                                                             (let [id (.data ele "id")
                                                                   label (.data ele "label")]
                                                               (or label id)))
                                                    :text-valign "center"
                                                    :color "#000000"
                                                    ;:background-color "#72a4ff"
                                                    :background-color (fn [ele]  ;"data(id)"
                                                                        (let [ttype (.data ele "triplet-type")]
                                                                          (if (= ttype "predicate-cluster")
                                                                            "#FFFFFF"
                                                                            "#bfffbf")))

                                                    :shape (fn [ele]
                                                             (let [shape (.data ele "shape")]
                                                               (or shape "circle")))
                                                    :width (fn [ele]
                                                             (let [width (.data ele "width")]
                                                               (or width 50)))
                                                    :height (fn [ele]
                                                             (let [height (.data ele "height")]
                                                               (or height 50)))
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
    (.use cytoscape (clj->js cose-bilkent))
    (tap> [:init-cy (:triple-set graph)])
    (doall (map add-triple! (:triple-set graph)))
    ;(update-cytoscape! graph)
    )
  )
