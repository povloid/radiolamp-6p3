(ns r6p3s.cpt.edit-form-for-id
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.net :as rnet]
            [r6p3s.core :as c]
            [r6p3s.cpt.alert :as alert]))




(def app-init
  {:id nil})



(defn component [app owner {:keys [chan-load-for-id
                                   uri uri-params+
                                   chan-load-row
                                   chan-save
                                   uri-save
                                   post-save-fn
                                   form-body
                                   fill-app-fn
                                   app-to-row-fn
                                   ]
                            :or   {url-params+ {}
                                   fill-app-fn
                                   (fn [row]
                                     (println "Функция (fill-app-fn) формирования зароса не определена!"
                                              " Пришел ответ вида: " row))
                                   app-to-row-fn
                                   (fn []
                                     (println "Функция (app-to-row-fn) формирования зароса не определена!")
                                     {})
                                   }}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-init-row (chan)})
    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-init-row]} (om/get-state owner)]

        (when chan-load-for-id
          (go
            (while true
              (let [id (<! chan-load-for-id)
                    id (if (= id 0) nil id)]
                ;; TODO: сделать отлов ошибок на alert
                (rnet/get-data
                 uri
                 (merge {:id id} uri-params+)
                 (fn [row]
                   (put! chan-init-row row) ))))))

        (when chan-load-row
          (go
            (while true
              (let [row (<! chan-load-row)]
                (put! chan-init-row row) ))))

        (when chan-init-row
          (go
            (while true
              (let [row (<! chan-init-row)]

                (om/update! app :id (or (:id row) nil))

                (alert/clean app)
                ;; TODO: сделать отлов ошибок на alert
                (fill-app-fn row) ))))

        (when chan-save
          (go
            (while true
              (let [_ (<! chan-save)]
                (println "SAVE!")

                (try
                  (if uri-save
                    ;; если указан то сохранять в сеть
                    (rnet/get-data
                     uri-save
                     (let [a (app-to-row-fn)]
                       (if-let [id (@app :id)]
                         (assoc a :id id)
                         a))
                     (fn [result]
                       (when post-save-fn (post-save-fn result))
                       ;; SUCCESS MESSAGE
                       (om/transact!
                        app #(assoc % :alert-danger nil :alert-success "Сохранено успешно")))

                     (fn [error-response]
                       (om/transact!
                        app #(assoc % :alert-danger (str "Ошибка сохранения: " error-response)))))

                    ;; иначе альтернитиваная функция
                    (when post-save-fn
                      (do (post-save-fn app))
                      ;; SUCCESS MESSAGE
                      (om/transact! app #(assoc % :alert-danger nil :alert-success "Сохранено успешно"))))

                  (catch js/Error e
                    (let [m (str "Ошибка сохранения: " e)]
                      (println m)
                      (om/update! app :alert-danger m))))))))


        ))
    om/IRender
    (render [_]
      (dom/div ;; Common mistakes - Form inside of another form
       #js {:className "form-horizontal col-xs-12 col-sm-12 col-md-12 col-lg-12"
            :style     #js {:paddingTop 6}}
       ;; HELPER FOR MESSAGES
       ;; Такой вариант сбивает форму в диалогах
       ;;(om/build alert/component app)

       (if form-body
         form-body
         (dom/h1 nil "Элементы формы еще не определены"))

       (dom/br nil)
       ;; HELPER FOR MESSAGES
       (om/build alert/component app)))))
