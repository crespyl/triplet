(ns app.renderer.cytoscape.core
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   ["cytoscape" :as cytoscape]
   ["cytoscape-cola" :as cola]
   ["cytoscape-fcose" :as fcose]
   ["cytoscape-cose-bilkent" :as cose-bilkent]
   ["cytoscape-automove" :as automove]
   [app.renderer.cytoscape.events :as events]
   [app.renderer.cytoscape.style :as style]))

(defn setup-events! [cy]
  (events/setup-default-events cy))

(defn make-cytoscape-config [element]
  {:container element
   :style (style/default-style)})

(defn enable-automove! [cy]
  (.automove cy (clj->js {
                          :nodesMatching (fn [node] (and (not (.locked node))
                                                         (.hasClass node "predicate")))
                          :reposition "mean"
                          }))
  cy)

(defn disable-automove! [cy]
  (.automove cy "destroy")
  cy)

(defn re-apply-automove! [cy]
  (when (.-automove cy)
    (disable-automove! cy))
  (enable-automove! cy)
  cy)

(defn node-to-cytoscape-node
  "Transforms node-id into a hash with additional data for use in Cytoscape,
  with optional additional metadata passed in the second parameter"
  ([node-id]
   (node-to-cytoscape-node node-id nil))
  ([node-id meta]
   (let [data (merge {:id node-id
                      :weight 1
                      :shape "round-rectangle"
                      :width (* 11 (count node-id))
                      :height 30
                      :name node-id ;(string/capitalize node-id)
                      :triplet-type :node
                      }
                     meta)]
     {:group "nodes"
      :classes "entity"
      :data data})))

(defn triple-to-cytoscape-edge
  "Transforms a triple of [node-id rel-id node-id] into a single Cytoscape
  element representing just that edge (i.e. not including the nodes)"
  [[subj pred obj]]
  (identity {:group "edges"
             :classes ["triple-edge"]
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
  (identity {:group "nodes"
             :classes ["predicate"]
             :data {
                    :id (hash [subj pred])
                    :label pred
                    :pred-type pred
                    :shape "ellipse"
                    :width (* 12 (count pred))
                    :height 20
                    :weight 1
                    }}))

(defn edge-id
  ([src tgt]
   (hash [:edge src tgt])))

(defn make-cytoscape-edge
  ([src tgt] (make-cytoscape-edge src tgt :default))
  ([src tgt class]
   (let [src-id (-> src :data :id)
         tgt-id (-> tgt :data :id)]
     (tap> [:make-edge src-id tgt-id (edge-id src-id tgt-id)])
     (identity {:group "edges"
                :classes [class]
                :data {
                       :id (edge-id src-id tgt-id)
                       :source src-id
                       :target tgt-id
                       :directed true
                       }}))))

(defn triple-to-cytoscape-elements
  "Transforms a triple of [node-id rel-id node-id] into a vec of hashes
  representing Cytoscape elements, reifying the relation into [[node -> rel],
  [rel -> node]] pairs"
  [[subj pred obj]]
  (let [subj-ele (node-to-cytoscape-node subj)
        obj-ele  (node-to-cytoscape-node obj)
        pred-ele (triple-to-cytoscape-node [subj pred obj])]
    [subj-ele (make-cytoscape-edge subj-ele pred-ele :outbound)
     pred-ele (make-cytoscape-edge pred-ele obj-ele :inbound)
     obj-ele]
    ))

;;(let [layout-params {
              ;;                  :name name
            ;;                    :animate true
            ;;                    :refresh 20
            ;;                    }]
            ;; (set! layout-name name)
            ;; (disable-automove! cy)
            ;; (.run (.layout
            ;;        (.elements cy)
            ;;        (clj->js layout-params)))
            ;; (enable-automove! cy)
            ;; cy)

(defn add-triple! [cy triple]
  (let [eles (triple-to-cytoscape-elements triple)
        jseles (.add cy (clj->js eles))]
    (let [params (clj->js { :name "cose" :animate true :refresh 20 })]
      ;(tap> [:relayout params eles])
      ;(.run (.layout (.neighborhood jseles) params))
      (.flashClass jseles "flash-new" 2000))
    cy))

(defn-  get-element [cy id] (.getElementById cy id))

(defn remove-triple!
  "Remove a triple from the Cytoscape graph.
  To do this, we need to remove at least one edge (the second one), and
  possibly the reified predicate node, if there are no other nodes left
  connected to it (the first edge automatically gets removed if/when we remove
  the predicate node).

  If the triple is `[node-a -1> pred-b -2> node-c]`, then we remove link `-2>`,
  check `pred-b` for edges, and remove `pred-b` if it has one (or fewer)
  remaining."

  ([cy [subj pred obj]]
   (let [pred-node-id (hash [subj pred])
         pred-node (get-element cy pred-node-id)
         link-2-id (edge-id pred-node-id obj)
         link-2    (get-element cy link-2-id)]
     (.remove cy link-2) ; remove 2nd edge
     (when (<= (count (.connectedEdges pred-node)) ; check number of edges on predicate node
               1)
       (.remove cy pred-node)))
     cy))

(defn relayout
  "Triggers the Cytoscape view to re-apply the selected layout"
  [cy name] (let [layout-params {
                               :name name
                               :animate true
                               :refresh 20
                               }]
              (disable-automove! cy)
              (.run (.layout
                     (.elements cy)
                     (clj->js layout-params)))
              (enable-automove! cy)
              cy))

(defn graph-to-cytoscape
  "converts a list of entity ids/names and a set of triples to an object with
  cytoscape-ready elements and edges"
  [graph]
  (let [nodes (map node-to-cytoscape-node (:entity-ids graph))
        edges (map triple-to-cytoscape-edge (:triple-set graph))]
    {:nodes nodes
     :edges edges}))

(defn remove-node!
  "Remove a node from the Cytoscape graph"
  ([cy node]
   (let [ele (.getElementById cy node)]
     (if-not (empty? ele)
       (do
         (.remove cy ele)
         true)
       false))))

(defn update-cytoscape! [cy graph]
  (let [cy-graph (graph-to-cytoscape graph)
        nodes (clj->js (flatten (:nodes cy-graph)))
        edges (clj->js (flatten (:edges cy-graph)))]
    (.add cy nodes)
    (.add cy edges)
    ))

(defn register-extensions! [_]
  (.use cytoscape (clj->js cola))
  (.use cytoscape (clj->js fcose))
  (.use cytoscape (clj->js cose-bilkent))
  (when-not (.-automove cytoscape)
    (tap> (.-automove cytoscape))
    (.use cytoscape (clj->js automove))))

(defn init-cytoscape [container graph]
  (let [el (js/document.getElementById container)
        cy  (cytoscape (clj->js (make-cytoscape-config el)))]
    (doseq [triple (:triple-set graph)]
      (add-triple! cy triple))
    cy))

(defn remount [cy container]
  (.mount cy (js/document.getElementById container)))
