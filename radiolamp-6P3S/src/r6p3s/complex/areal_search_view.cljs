(ns r6p3s.complex.areal-search-view
  ;;(:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.net :as rnet]
            [r6p3s.core :as rc]
            [r6p3s.cpt.modal :as modal]
            [r6p3s.cpt.modal-yes-no :as modal-yes-no]
            [r6p3s.cpt.actions-modal :as actions-modal]
            [r6p3s.cpt.search-view :as search-view]
            [r6p3s.ui.table :as table]
            [r6p3s.ui.button :as button]
            [r6p3s.ui.thead-tr :as thead-tr]
            [r6p3s.ui.media :as media]
            [r6p3s.ui.media-object :as media-object]
            [r6p3s.ui.glyphicon :as glyphicon]
            [r6p3s.ui.th-title :as th-title]
            [r6p3s.cpt.tbody-trs-sel :as tbody-trs-sel]
            [r6p3s.cpt.text-collapser :as text-collapser]

            [r6p3s.complex.areal-modal-edit-form :as modal-edit-form]))






(defn media-object-render [logotype px]
  (if (empty? logotype)
    (glyphicon/render "tree-conifer" nil (str px "px"))
    (media-object/render
     {:class+ ""
      :src (str logotype "_as_" px ".png")
      :style #js {:width px}})))




(defn row-view [app _ {:keys []}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [id logotype keyname path_keynames description]} @app]
        (media/render
         {:heading (dom/h4 #js {:className "text-primary"} keyname)

          :media-object (media-object-render logotype 60)
          :body         (dom/div nil
                                 (dom/span #js {:className "text-info"} path_keynames)
                                 (dom/hr #js {:style #js {:marginTop 4 :marginBottom 4}})
                                 (om/build text-collapser/component app {:opts {:k :description}}))})))))


(defn td-view [app _ opts]
  (reify
    om/IRender
    (render [_]
      (dom/td
       #js {:style #js {:whiteSpace "normal"}}
       (om/build row-view app {:opts opts})))))












(def app-init
  (merge
   search-view/app-init
   {:cut-buffer   nil
    :modal-add    modal-edit-form/app-init
    :modal-act    actions-modal/app-init
    :modal-yes-no (assoc modal-yes-no/app-init :row {})
    }))

(defn component [app _ {:keys [selection-type editable?]
                        :or   {selection-type :one}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update       (chan)
       :chan-modal-act    (chan)
       :chan-modal-add-id (chan)})
    om/IRenderState
    (render-state [_ {:keys [chan-modal-act
                             chan-modal-add-id
                             chan-update]}]
      
      (letfn [(cut-buffer-clear []
            (om/update! app :cut-buffer nil))
          (cut-buffer-paste [parent_id]
            (if-let [child_id (get-in @app [:cut-buffer :id])]
              (rnet/get-data
               "/camerton/rb/areal/save"
               {:row {:id child_id :parent_id parent_id}}
               (fn [_]
                 (om/update! app :cut-buffer nil)
                 (put! chan-update 1)))
              
              ;; запомнить элемент
              (->> @app
                   search-view/selected-first
                   (om/update! app :cut-buffer)))

            (modal/hide (:modal-act app)))]
        
        (dom/div nil
                 (when-let [cut-buffer (@app :cut-buffer)]
                   (dom/div #js {:style #js {:padding 6}}
                            (dom/div #js {:style #js {:float "right"}}                                     
                                     (button/render {:text     (dom/span nil (glyphicon/render "fast-backward") " в корень")
                                                     :type     :warning
                                                     :class+   "btn-block"                                           
                                                     :on-click #(cut-buffer-paste nil)})
                                     (button/render {:text     (dom/span nil (glyphicon/render "remove") " отмена")
                                                     :class+   "btn-block"                                            
                                                     :on-click cut-buffer-clear}))
                            (media/render
                             {:heading      (dom/b #js {:className "text-warning"} "Для перемещения выбран элемент")
                              :media-object (glyphicon/render "copy" "text-warning" "5em")
                              :body         (let [{:keys [id logotype keyname path_keynames description]} cut-buffer]
                                              (media/render
                                               {:heading      (dom/span #js {:className "text-primary"} keyname)
                                                :media-object (media-object-render logotype 30)
                                                :body         (dom/span #js {:className "text-info"} path_keynames)}))})))
                 (om/build search-view/component app
                           {:opts
                            {:chan-update chan-update
                             :data-update-fn
                             (fn [app]
                               (rnet/get-data "/camerton/rb/areal/list"
                                              {:fts-query (get-in @app [:fts-query :value])
                                               :page      (get-in @app [:page])}
                                              (fn [response]
                                                (om/update! app :data (vec response)))))
                             :data-rendering-fn
                             (fn [app-2]
                               (table/render {:hover?      true
                                              :bordered?   true
                                              :striped?    true
                                              :responsive? true
                                              :thead       (thead-tr/render [(th-title/render
                                                                              {:colspan    1
                                                                               :class-name "rb-th-title"
                                                                               :icon       "icon-cam-location2"
                                                                               :title      "Географические области"})])
                                              :tbody
                                              (om/build tbody-trs-sel/component  (:data app-2)
                                                        {:opts {:selection-type selection-type
                                                                :app-to-tds-seq-fn
                                                                (fn [row]
                                                                  [(om/build td-view row)])
                                                                :on-select-fn
                                                                (fn [{:keys [id parent_id] :as row}]
                                                                  (when editable?
                                                                    (put! chan-modal-act
                                                                          {:label (str "Выбор действий над записью №" id)
                                                                           :acts
                                                                           [{:text     "Добавить в подгруппу"
                                                                             :btn-type :primary
                                                                             :act-fn   (fn []
                                                                                         (println ">>>" id parent_id)
                                                                                         (om/update! app [:modal-add :parent_id] id)
                                                                                         (put! chan-modal-add-id 0)
                                                                                         (modal/show (:modal-add app)))}

                                                                            {:text     "Редактировать"
                                                                             :btn-type :primary
                                                                             :act-fn   (fn []
                                                                                         (println ">>>" id parent_id)
                                                                                         (om/update! app [:modal-add :parent_id] parent_id)
                                                                                         (put! chan-modal-add-id id)
                                                                                         (modal/show (:modal-add app)))}
                                                                            {:text     "Удалить"
                                                                             :btn-type :danger
                                                                             :act-fn   #(do
                                                                                          (om/update! app [:modal-yes-no :row] row)
                                                                                          (modal/show (:modal-yes-no app)))}
                                                                            {:text   (if (:cut-buffer @app) "Вставить" "Вырезать")
                                                                             :act-fn #(cut-buffer-paste id)}]
                                                                           }))
                                                                  )
                                                                }})
                                              }))
                             :add-button-fn
                             #(do (modal/show (:modal-add app))
                                  (om/update! app [:modal-add :parent_id] nil)
                                  (put! chan-modal-add-id 0))
                             }})

                 (when editable?
                   (om/build actions-modal/component (:modal-act app) {:opts {:chan-open chan-modal-act}}))

                 (om/build modal-edit-form/component (:modal-add app)
                           {:opts {:label            "Работа с записью"
                                   :chan-load-for-id chan-modal-add-id
                                   :post-save-fn     #(do
                                                        (when chan-update
                                                          (put! chan-update 1))
                                                        1)
                                   }})

                 (when editable?
                   (om/build modal-yes-no/component (:modal-yes-no app)
                             {:opts {:modal-size :sm
                                     :label      "Желаете удалить запись?"
                                     :body
                                     (dom/div
                                      #js{:className "row"}
                                      (dom/h3 #js{:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"}
                                              (get-in @app [:modal-yes-no :row :keyname])))
                                     :act-yes-fn
                                     (fn []
                                       (rnet/get-data
                                        "/camerton/rb/areal/delete"
                                        {:id (get-in @app [:modal-yes-no :row :id])}
                                        (fn [_]
                                          (when chan-update
                                            (put! chan-update 1)))))
                                     }}))

                 )))))
