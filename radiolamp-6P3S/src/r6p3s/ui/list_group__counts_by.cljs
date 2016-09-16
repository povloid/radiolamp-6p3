(ns r6p3s.ui.list-group--counts-by
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.ui.list-group :as list-group]
            [r6p3s.ui.list-group-item :as list-group-item]))


(defn render [points f
              {:keys [text nil-text order-fn order-direction
                      show-percents? show-numeration?]
               :or   {text     "метка в заголовке"
                      nil-text "не указано"}}]
  (let [items (->> points
                   (group-by f)
                   seq)

        items        (if order-fn
                       (let [v (sort-by (comp order-fn second) items)]
                         (if (= order-direction :desc)
                           (reverse v)
                           v))
                       items)
        points-count (count points)]
    (list-group/render
     (->> items
          (map-indexed #(conj %2 (inc %1)))
          (reduce
           (fn [a [gi items i]]
             (let [items-count (count items)]
               (conj a (list-group-item/render
                        {:text  (dom/span nil
                                          (when show-numeration?
                                            (dom/div #js {:style #js {:color    "#bbb"
                                                                      :minWidth 25
                                                                      :float    "left"}}
                                                     i ". "))
                                          (or gi nil-text))
                         :badge (dom/span nil
                                          items-count
                                          (when show-percents?
                                            (dom/span #js {:style #js {:color "#aaa"}}" | "))
                                          (when show-percents?
                                            (dom/span #js {:style #js {:color "#bfb"}}
                                                      (.toFixed (/ items-count (/ points-count  100)) 2) "%")))
                         :type  (if (nil? gi) :warning :default)}))))
           [(list-group-item/render
             {:text    text
              :active? true
              :badge   (dom/span nil  points-count
                                 (when show-percents?
                                   (dom/span #js {:style #js {:color "#aaa"}}" | "))
                                 (when show-percents?                              
                                   (dom/span #js {:style #js {:color "#373"}} "100%")))})]
           )))))
