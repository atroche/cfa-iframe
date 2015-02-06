(ns iframe-app.journeys
  (:require-macros [cljs.core.typed :refer [ann]])
  (:require [cljs.core.typed]
            [iframe-app.core :refer [update-conditions]]
            [iframe-app.utils :refer [field-values-from-fields]]))


(defmulti transform :action-type)

(defmethod transform :select-master-field
  [action state]
  (assoc state :selections {:master-field (:value action)
                            :field-value  nil
                            :slave-fields #{}}))

(defmethod transform :select-field-value
  [action state]
  (-> state
      (assoc-in [:selections :field-value] (:value action))
      (assoc-in [:selections :slave-fields] #{})))

(defmethod transform :toggle-slave-field
  [{:keys [value]} state]
  (let [slave-field-already-toggled? ((get-in state [:selections :slave-fields]) value)
        update-set-fn (if slave-field-already-toggled? disj conj)
        new-state (update-in state [:selections :slave-fields] update-set-fn value)
        newer-state (update-in new-state [:conditions] update-conditions)]
    newer-state))


(defrecord Action [action-type value]

  IPrintWithWriter
  (-pr-writer [o writer _] (-write writer "I'M AN ACTION")))



(def starting-state {:selections {:master-field nil
                                  :field-value  nil
                                  :slave-fields #{}}
                     :conditions #{}})

(ann possible-selection-states [#{TicketField} -> #{SelectionState}])
(defn possible-selection-states [ticket-fields]
  {:post [(contains? % starting-state)
          (not-empty %)]}
  "Returns a set of possible states that {:master-field field :field-value value}
   could be in."
  (set
    (let [field-values (field-values-from-fields ticket-fields)]
      (for [field ticket-fields
            field-value field-values
            master-nil? [false true]
            value-nil? [false true]]
        {:selections {:master-field (if-not master-nil? field)
                      :field-value  (if-not value-nil? field-value)
                      :slave-fields #{}}
         :conditions #{}}))))

(defn categorize-state [{:keys [selections] :as state}]
  (let [{:keys [master-field field-value]} selections]
    (cond (= starting-state state) :start
          (and (not-empty master-field) (nil? field-value)) :on-master-field
          (every? not-empty [master-field field-value]) :on-field-value
          :else :error)))


(defn next-actions-for-state [{:keys [selections] :as state} ticket-fields]
  {:post [(every? (fn [action] (= Action (type action))) %)]}
  (let [{:keys [master-field field-value]} selections
        state-category (categorize-state state)
        other-fields (remove #(= % master-field) ticket-fields)
        available-field-values (:possible-values master-field)
        other-field-values (remove #(= % field-value) available-field-values)]
    (let [values->actions (fn [values action-type]
                            (for [value values]
                              (->Action action-type value)))
          next-actions (case state-category
                         :start (values->actions ticket-fields :select-master-field)
                         :on-master-field (concat (values->actions other-fields :select-master-field)
                                                  (values->actions available-field-values :select-field-value))
                         :on-field-value (concat (values->actions other-fields :select-master-field)
                                                 (values->actions other-field-values :select-field-value)
                                                 (values->actions other-fields :toggle-slave-field))
                         '())]
      next-actions)))

(defn choose-action [actions]
  (rand-nth actions))

(defn generate-user-journey [ticket-fields]
  {:post [(every? (fn [action] (= Action (type action))) %)]}
  (loop [state starting-state
         path []]
    (let [possible-actions (next-actions-for-state state ticket-fields)]
      (if (or (> (count path) 3) (empty? possible-actions))
        path
        (let [action (choose-action possible-actions)]
          (recur (transform action state)
                 (conj path action)))))))

