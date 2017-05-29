(ns r6p3s.ui.diff-panel
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.ui.glyphicon :as glyphicon]
            [r6p3s.ui.table :as table]
            [r6p3s.ui.group-box :as group-box]))




(declare show-value)


(defn render
  [diff-state {:keys [class-name
                      style
                      filter-pred-fn
                      fields-map]
               :or   {filter-pred-fn (fn [_] true)
                      fields-map     {}}}]
  (when-not (empty? diff-state)
    (let [[b a _] diff-state]
      (->> (set (into (keys a) (keys b)))
           (filter filter-pred-fn)
           (map (fn [k]
                  (let [a? (not (empty? a))
                        b? (not (empty? b))

                        {:keys [text] :as meta} (fields-map k)
                        icon                    (cond (and a? b?) "arrow-right text-warning"
                                                      a?          "minus text-primary"
                                                      b?          "plus text-danger"
                                                      :else       "asterisk")]
                    (dom/tr
                     #js {}
                     (dom/td #js {:style #js {:textAlign "right"}}
                             (dom/b nil
                                    (or text (str k))))
                     
                     (when a?
                       (dom/td nil
                               (show-value meta (get a k))))
                     
                     (dom/td nil
                             (glyphicon/render icon))
                     (when b?
                       (dom/td nil
                               (show-value meta (get b k))))))))
           (apply dom/tbody nil)
           (dom/table #js {:className class-name
                           :style     style})))))




(defmulti show-value (fn [{type :type} value] type))

(defmethod show-value :default
  [_ value]
  (cond
    (nil? value)   "ничего"
    (true? value)  "да"
    (false? value) "нет"
    (map? value)   (if-let [text (:text value)]
                     text
                     (if-let [content-type (:content_type value)]
                       (condp = content-type
                         "image/jpeg" (dom/img #js {:src (str (:path value) "_as_60.png")})
                         (str value))))
    :else          (str value)))







