(ns r6p3s.ui.list-group--counts-by
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.ui.list-group :as list-group]
            [r6p3s.ui.list-group-item :as list-group-item]))


(defn render [points f
              {:keys [text nil-text order-fn order-direction]
               :or   {text     "метка в заголовке"
                      nil-text "не указано"}}]
  (let [items (->> points
                   (group-by f)
                   seq)

        items (if order-fn
                (let [v (sort-by (comp order-fn second) items)]
                  (if (= order-direction :desc)
                    (reverse v)
                    v))                
                items)]
    (list-group/render
     
     (reduce
      (fn [a [gi items]]
        (conj a (list-group-item/render
                 {:text (or gi nil-text) :badge (count items)
                  :type (if (nil? gi) :warning :default)})))
      [(list-group-item/render {:text text :active? true :badge (count points)})]
      items))))
