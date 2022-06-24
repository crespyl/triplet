(ns app.renderer.loader
  (:require [clojure.string :as string]
            [re-frame.core :as rf]))


(def demo-csv
  "Subject, Predicate, Object
Concept Maps, represent, Organized Knowledge
Concept Maps, help to answer, Focus Questions
Focus Questions, are, Context Dependent
Context Dependent, e.g., Personal
Context Dependent, e.g., Social
Organized Knowledge, needed to answer, Focus Questions
Organized Knowledge, is, Context Dependent
Organized Knowledge, necessary for, Effective Teaching
Organized Knowledge, necessary for, Effective Learning
Organized Knowledge, includes, Associated Feelings or Affect
Organized Knowledge, is comprised of, Concepts
Organized Knowledge, is comprised of, Propositions
Associated Feelings or Affect, add to, Concepts
Concepts, connected using, Linking Words
Linking Words, used to form, Propositions
Concepts, are, Perceived Regularities or Patterns
Concepts, are, Labeled
Concepts, are, Hierarchically Structured
Propositions, are, Hierarchically Structured
Propositions, are, Units of Meaning
Propositions, may be, Crosslinks
Perceived Regularities or Patterns, in, Events (Happenings)
Perceived Regularities or Patterns, in, Objects (Things)
Perceived Regularities or Patterns, begin with, Infants
Labeled, with, Symbols
Labeled, with, Words
Hierarchically Structured, aids, Creativity
Hierarchically Structured, especially with, Experts
Hierarchically Structured, in, Cognitive Structure
Units of Meaning, constructed in, Cognitive Structure
Crosslinks, show, Interrelationships
Creativity, begins with, Infants
Creativity, needed to see, Interrelationships
Interrelationships, between, Different Map Segments")

(defn simple-parse [csv-str]
  (map (fn [s]
         (vec (map #(string/replace % " " "-")
                   (map string/trim
                        (string/split s ",")))))
       (string/split-lines csv-str)))

(defn load-demo []
 (let [rows (simple-parse demo-csv)]
  (doseq [r (drop 1 rows)] (rf/dispatch [:add-statement r]))))
