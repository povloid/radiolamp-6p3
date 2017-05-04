(ns r6p3.dataform-search
  (:require [korma.db :as kdb]
            [korma.core :as kc]))



(declare make-pred)

(defn pred
  "Построение предиката над запросом из выбранных данных поисковой датаформы"
  [query
   {:keys [on?] :as dataform-search-selected}
   {:keys [realtype-field fields] :as rbs-schema}]
  (if-not on?
    query
    (let [{:keys [realtype selectors]} dataform-search-selected]
      (reduce
       (fn [query {:keys [field selected]}]
         (make-pred query (fields field) field selected))
       (kc/where query (= realtype-field (name realtype)))
       selectors))))



(defmulti make-pred (fn [_ {{type :type} :search} _ _] type))


(defmethod make-pred :default
  [query meta field selected]
  ;;(println "Неопределенный: " field meta selected)
  query)



(defmethod make-pred :multi-buttons
  [query meta field selected]
  (if (empty? selected)
    query
    (let [{eqs := eq-and-up :<= :as a} (group-by :cmp selected)]
      (kc/where query (or
                       (when-not (empty? eqs)
                         (in field (map :val eqs)))
                       (when-not (empty? eq-and-up)
                         (apply or (map (fn [{:keys [val]}]
                                          (<= val field))
                                        eq-and-up))))))))


(defmethod make-pred :band-integer-to
  [query _ field selected]
  (println selected)
  (if selected
    (kc/where query (<= field selected))
    query))


(defmethod make-pred :band-integer-from
  [query _ field selected]
  (if selected
    (kc/where query (<= selected field))
    query))


(defmethod make-pred :band-integer-from-to
  [query _ field {:keys [from to]}]
  (-> query
      
      (as-> query
          (if from
            (kc/where query (<= from field))
            query))

      (as-> query
          (if to
            (kc/where query (<= field to))
            query))))


(defmethod make-pred :boolean
  [query _ field selected]
  (kc/where query (= field selected)))


(defmethod make-pred :boolean-nm
  [query _ field selected]
  (condp = selected
    :n (kc/where query {field false})
    :y (kc/where query {field true})
    ;; nm
    query))


(defmethod make-pred :rbs-multi-select
  [query meta field selected]
  (if (empty? selected)
    query
    (kc/where query (in field selected))))


