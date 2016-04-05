(ns ix.omut.ui.list-group--counts-by
  (:require [om.dom :as dom :include-macros true]
            [ix.omut.ui.list-group :as list-group]
            [ix.omut.ui.list-group-item :as list-group-item]))


(defn render [points f
              {:keys [text nil-text]
               :or   {text     "метка в заголовке"
                      nil-text "не указано"}}]
  (let [items (->> points (group-by f) seq)]
    (list-group/render
     (reduce
      (fn [a [gi items]]
        (conj a (list-group-item/render
                 {:text (or gi nil-text) :badge (count items)
                  :type (if (nil? gi) :warning :default)})))
      [(list-group-item/render {:text text :active? true :badge (count points)})]
      items))))
