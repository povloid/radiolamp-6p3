(ns r6p3s.complex.dataform-info-panel
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.ui.panel-with-table :as panel-with-table]))

(defn render
  "Визуальный компонент для отображения данных датаформы в виде информационной панели"
  [{:keys [realtype]
    :as   row}
   rbs-scheme]
  (let [rbs-scheme  rbs-scheme
        show        (into
                     (get-in rbs-scheme [:common :fields] #{})
                     (get-in rbs-scheme [:realtype realtype :fields] #{}))]

    (->> rbs-scheme
         :fields
         seq
         (filter (comp show first))
         (group-by (comp :field-group second))
         seq
         (map
          (fn [[field-group fields]]
            (let [{:keys [ord text
                          icon type]} (get-in rbs-scheme
                                              [:fields-groups field-group])]
              [ord
               (panel-with-table/render
                {:heading-font-icon icon
                 :heading           text
                 :type              type
                 ;;:body (dom/p nil "")
                 ;;:cols ["параметр" "значение"]
                 :rows
                 (map
                  (fn [[field-k {:keys [in-row type text]}]]
                    (let [v (row field-k)]
                      [text
                       (condp = type
                         :money   (str v)
                         :integer (str v)
                         :string  (str v)
                         :text    (str v)
                         :boolean (str (when v (if v "да" "нет")))
                         :rbs     (str (get-in row [in-row :keyname]))
                         (str v))]))
                  fields)})])))
         (sort-by first)
         (map second)
         (apply dom/div nil))))
