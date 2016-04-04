(ns ix.omut.input-from-search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.io  :as ix-io]
            [ix.net :as ixnet]
            [ix.omut.core :as c]
            [ix.omut.common-input :as common-input]
            [ix.omut.ui.table :as table]
            [ix.omut.ui.tbody :as tbody]
            [ix.omut.ui.thead-tr :as thead-tr]
            [ix.omut.component.input :as input]
            [ix.omut.component.helper-p :as helper-p]
            [ix.omut.component.search-view :as search-view]
            [ix.omut.component.modal :as modal]
            [ix.omut.component.tbody-trs-sel :as tbody-trs-sel]))




;;**************************************************************************************************
;;* BEGIN Ввод элементов из справочника
;;* tag: <input rb>
;;*
;;* description: Элементы выбора из справочной таблици
;;*
;;**************************************************************************************************

(defn app-init [search-view/app-init]
  {:modal (assoc modal/app-init
                 :search-view search-view/app-init)
   :sel   []
   })

(defn get-selected [app]
  (:sel app))

(defn clean [app]
  (assoc app :sel []))


(defn component [search-view
                 {:keys [label-one
                         label-multi
                         placeholder
                         class+
                         on-selected-fn
                         ui-type
                         ui-type--add-button--type
                         ui-type--add-button--text
                         selection-type
                         disabled?
                         multiselect-row-render-fn
                         row-pk-fiels
                         one--row-to-text-fn
                         multi-table-caption
                         ]
                  :or   {class+                    ""
                         selection-type            :one
                         label-one                 "Выбрать ???"
                         label-multi               "Выбрано ???"
                         placeholder               "Выберите...."
                         ui-type                   :input-select
                         ui-type--add-button--type :default
                         ui-type--add-button--text "Выбрать..."
                         row-pk-fiels              [:id]
                         multi-table-caption       "Наименование"
                         }}]
  (fn [app _ {:keys [label-one
                     label-multi
                     selection-type
                     ui-type
                     ui-type--add-button--type
                     ui-type--add-button--text
                     on-selected-fn
                     label-class+
                     input-class+
                     search-view-opts
                     main-div-params
                     multi-table-caption]
              :or   {label-one                 label-one
                     label-multi               label-multi
                     selection-type            selection-type
                     ui-type                   ui-type
                     ui-type--add-button--type ui-type--add-button--type
                     ui-type--add-button--text ui-type--add-button--text
                     on-selected-fn            on-selected-fn
                     label-class+              "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                     input-class+              "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                     search-view-opts          {}
                     multi-table-caption       multi-table-caption}}]


    (reify
      om/IRender
      (render [_]
        (dom/div
         main-div-params

         ;;(dom/p nil (str (@app :sel)))

         (condp = ui-type

           :add-button
           (c/ui-button {:type     ui-type--add-button--type
                         :on-click #(modal/show (:modal app))
                         :text     ui-type--add-button--text })

           :input-select
           (dom/div #js {:className (str "form-group " class+ " "(common-input/input-css-string-has? @app))}
                    (dom/label #js {:className (str "control-label " label-class+)}
                               ({:one label-one :multi label-multi} selection-type))

                    (condp = selection-type

                      :one
                      (dom/div
                       #js {:className input-class+}
                       (dom/div #js {:className "input-group"}
                                (dom/input #js {:value       (let [r (get-in @app [:sel 0])]
                                                               (if one--row-to-text-fn
                                                                 (one--row-to-text-fn r)
                                                                 (:keyname r)))
                                                :placeholder placeholder
                                                :className   "form-control"})
                                (dom/span #js {:className "input-group-btn"}
                                          (c/ui-button {:type     :default
                                                        :on-click #(modal/show (:modal app))
                                                        :text     (dom/span #js {:className   "glyphicon glyphicon-list-alt"
                                                                                 :aria-hidden "true"})})))
                       (om/build helper-p/component app))

                      :multi
                      (dom/div
                       #js {:className input-class+ :style #js {}}
                       (dom/div
                        #js {:className (str "panel "
                                             ({:muted   "panel-muted"
                                               :primary "panel-primary"
                                               :success "panel-success"
                                               :info    "panel-info"
                                               :warning "panel-warning"
                                               :danger  "panel-danger"}
                                              ui-type--add-button--type))}
                        (dom/div #js {:className "panel-heading"}
                                 (dom/b nil "Выбрано (" (count (@app :sel)) ")")
                                 (om/build helper-p/component app))
                        (dom/div #js {:className "panel-body" :style #js {:padding 2}}
                                 (table/render
                                  {:hover?    true
                                   :bordered? true
                                   :striped?  true
                                   ;;:responsive? true
                                   :style+    #js {:marginBottom 0}
                                   :thead     (thead-tr/render [(dom/th nil multi-table-caption) (dom/th nil "Действие")])
                                   :tbody     (om/build tbody-trs-sel/component (app :sel)
                                                        {:opts
                                                         {:app-to-tds-seq-fn
                                                          (fn [row]
                                                            (list
                                                             (if multiselect-row-render-fn
                                                               (multiselect-row-render-fn row)
                                                               (dom/td nil (str @row)))
                                                             (dom/td
                                                              #js {:style #js {:width "5%"}}
                                                              (c/ui-button
                                                               {:text "Удалить"
                                                                :on-click
                                                                (fn [_]
                                                                  (om/transact!
                                                                   app :sel
                                                                   (fn [selected]
                                                                     (let [pk (select-keys @row row-pk-fiels)]
                                                                       (->> selected
                                                                            (filter
                                                                             #(not
                                                                               (= (select-keys % row-pk-fiels)
                                                                                  pk)))
                                                                            vec))))

                                                                  1)}))))
                                                          }})
                                   }))

                        (dom/div #js {:className "panel-footer"}
                                 (c/ui-button {:type     ui-type--add-button--type
                                               :on-click #(modal/show (:modal app))
                                               :text     ui-type--add-button--text }))))



                      (str "Непонятный selection-type: " selection-type))




                    )

           (str "непонятный тип отображения: " ui-type))


         (om/build modal/component (:modal app)
                   {:opts {:modal-size :lg
                           :body       (om/build search-view (get-in app [:modal :search-view])
                                                 {:opts (merge {:selection-type selection-type} search-view-opts)})
                           :footer     (dom/div #js {:className "btn-toolbar  pull-right"}
                                                (c/ui-button
                                                 {:type :primary
                                                  :on-click
                                                  (fn [_]
                                                    (let [selected (->> @app
                                                                        :modal
                                                                        :search-view
                                                                        :data
                                                                        (filter c/omut-row-selected?))]

                                                      (condp = selection-type
                                                        :multi (om/transact!
                                                                app :sel
                                                                (fn [app]
                                                                  (->> app
                                                                       (map #(c/omut-row-set-selected! % false))
                                                                       (into selected)
                                                                       (reduce
                                                                        #(assoc %1
                                                                                (select-keys %2 row-pk-fiels)
                                                                                %2)
                                                                        {})
                                                                       vals
                                                                       (sort-by #(-> (select-keys % row-pk-fiels)
                                                                                     vals
                                                                                     sort
                                                                                     vec))
                                                                       reverse
                                                                       vec)))

                                                        :one   (om/update! app :sel (vec selected)))

                                                      (modal/hide (:modal app))

                                                      (when on-selected-fn (on-selected-fn))

                                                      1))
                                                  :text "Выбрать"})

                                                (c/ui-button {:on-click (fn [_] (modal/hide (:modal app)) 1)
                                                              :text     "Закрыть"})
                                                )
                           }})
         )))))






;; END Ввод элементов из справочника
;;..................................................................................................
