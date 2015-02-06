(ns iframe-app.generators
  (:require [cljs.test.check :as tc]
            [cljs.test.check.generators :refer [sample] :as gen]))

(defn hmap-gen [map-schema]
  "Takes map like {:whatever whatev-gen :another-key another-generator} and
   returns a generator."
  (gen/fmap (fn [tuple]
              (into {} (map vector (keys map-schema) tuple)))
            (apply gen/tuple (vals map-schema))))

(def non-empty-string (gen/such-that not-empty gen/string-alpha-numeric))

(def ints-used-so-far (atom #{}))

(def field-value-gen
  (hmap-gen {:name non-empty-string
             :value non-empty-string}))

(def ticket-field-gen
  (hmap-gen {:name non-empty-string
             :id gen/pos-int
             :possible-values (gen/not-empty (gen/vector field-value-gen))}))

(def ticket-fields-gen
  (gen/such-that
    (fn [ticket-fields]
      (and (> (count ticket-fields) 2)
           (= (count (map :id ticket-fields))
              (count (distinct (map :id ticket-fields))))))
    (gen/vector ticket-field-gen)
    1000))





