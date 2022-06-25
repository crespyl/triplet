(ns app.renderer.cytoscape.style)

(defn default-style []
  (identity
   [{
     :selector "node"
     :style {
             :label (fn [ele]           ;"data(id)"
                      (let [id (.data ele "id")
                            label (.data ele "label")]
                        (or label id)))
             :text-valign "center"
             :color "#000000"
             :background-color "#bfffbf"
             :shape (fn [ele]
                      (let [shape (.data ele "shape")]
                        (or shape "circle")))
             :width (fn [ele]
                      (let [width (.data ele "width")]
                        (or width 50)))
             :height (fn [ele]
                       (let [height (.data ele "height")]
                         (or height 50)))
             :z-index 100
             }}
    {:selector "node:selected"
     :style {
             :background-color "#8bff78"
             :border-color "green"
             :border-width 2
             :border-style "dashed"}}
    {:selector "node:locked"
     :style {
             :border-color "black"
             :border-width 2}}
    {:selector "node.predicate"
     :style {
             :border-width 0
             :background-opacity 0
             :shape "ellipse"
             :z-index 1}}
    {:selector "node.predicate:selected"
     :style {
             :border-width 1
             :border-color "#aaaaaa"}}
    {:selector "node.predicate:locked"
     :style {
             :border-color "#555555"
             :border-width 1
             }}
    {:selector "edge"
     :style {
             :width 2
             :line-color "black"
             :opacity "0.5"
                                        ;:label "data(label)"
             :curve-style "straight"
                                        ;:mid-target-arrow-shape "triangle"
                                        ;:mid-target-arrow-color "#000000"
             :transition-property "line-color opacity width"
             :transition-duration "1s"
             }}
    {:selector "edge.inbound"
     :style {
             :target-arrow-shape "triangle"
             :target-arrow-color "#000000"}}
    {:selector "edge[label]"
     :style {
             :label "data(label)"
             :text-rotation "autorotate"
             :text-margin-x "0px"
             :text-margin-y "10px"
             :text-valign "top"
             :text-halign "center"}}
    {:selector "edge.flash"
     :style {:line-color "red"}}
    {:selector "edge.flash-new"
     :style {:line-color "limegreen"
             :width 4
             :opacity 1
             :transition-property "line-color opacity width"
             :transition-duration "1s"}}
    ]))
