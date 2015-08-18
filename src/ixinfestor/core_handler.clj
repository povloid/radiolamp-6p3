(ns ixinfestor.core-handler

  (:use compojure.core)

  (:require [clojure.java.io :as io]
            [ring.middleware.reload :refer (wrap-reload)] ;; reload temlates
            [ring.middleware.json :as json]
            [ring.middleware.session :as session]
            [ring.middleware.multipart-params :as multipart]

            [compojure.handler :as handler]
            [compojure.route :as route]

            [ixinfestor.core :as ix]
            [ixinfestor.core-web :as cw]
            [ixinfestor.core-web-bootstrap :as cwb]

            [ixinfestor.transit :as transit]

            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])

            )
  )


(defn parseLong [v] (Long/parseLong v))

(defn do-in-if-not-nil? [params path do-fn]
  (if (get-in params path) (do-fn params) params))

(defn update-in-if-not-nil? [params path f & args]
  (if-let [v (get-in params path)]
    (assoc-in params path (apply (partial f v) args))
    params))

;;**************************************************************************************************
;;* BEGIN
;;* tag: <>
;;*
;;* description:
;;*
;;**************************************************************************************************

(defn routes-ix* [cms-roles-set
                  {:keys [page-ixcms-main-params]
                   :or {page-ixcms-main-params {}
                        }
                   :as spec}]
  (println "routes-ix* - spec>" spec)

  (routes

   (ANY "/ix/set" request
        (cw/ix-accoc-parametr request))

   (GET "/ixinfestor" request
        (friend/authorize
         cms-roles-set
         (-> request
             (cwb/page-ixcms-main page-ixcms-main-params)
             ring.util.response/response
             (ring.util.response/header "Content-Type" "text/html; charset=utf-8"))
         )
        )

   ))

;; END
;;..................................................................................................


;;------------------------------------------------------------------------------
;; BEGIN: Fule uploading routes
;; tag: <file upload routes>
;; description: Маршруты для файлового аплоадера
;;------------------------------------------------------------------------------


(defn rest-files-list [{{:keys [page page-size fts-query]
                         :or {page 1 page-size 10 fts-query ""}} :params}]
  (-> ix/files-select*
      (ix/com-pred-page* (dec page) page-size)

      (as-> query
          (let [fts-query (clojure.string/trim fts-query)]
            (if (empty? fts-query)
              query
              (ix/files-pred-search? query fts-query))))

      (korma.core/order :id :desc)
      ix/com-exec))


(defn rest-file-find [request]
  (->> request
       :params
       :id
       (ix/com-find ix/files)))

(defn rest-file-edit [request]
  (-> request
      :params
      ix/files-save))

(defn rest-file-delete [request]
  (-> request
      :params
      ix/files-delete
      ((fn [_] {:result "OK"}))))


(defn routes-file* [edit-roles-set {:keys [save-file-fn-options]}]
  (routes


   ;; JSON
   (POST "/files/list" request
         (-> request
             rest-files-list
             ring.util.response/response))
   ;; TRANSIT
   (POST "/files/list/transit" request
         (-> request
             rest-files-list
             transit/response-transit))



   (multipart/wrap-multipart-params
    (POST "/files/upload" request
          (friend/authorize
           edit-roles-set
           (do
             (ix/web-file-upload
              (fn [a b]
                (ix/file-upload-o a b
                                        ;TODO: ДОбавить выбор варианта от типа контента
                                  (merge {:path-prefix "/image/"} save-file-fn-options)))
              (-> request :params :image-uploader))
             (ring.util.response/response "OK") ))))


   ;; JSON
   (POST "/files/find" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-find
              ring.util.response/response)))

   ;; TRANSIT
   (POST "/files/find/transit" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-find
              transit/response-transit)))



   ;; JSON
   (POST "/files/edit" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-edit
              ring.util.response/response)))

   ;; TRANSIT
   (POST "/files/edit/transit" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-edit
              transit/response-transit)))


   ;; JSON
   (POST "/files/delete" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-delete
              ring.util.response/response
              cw/error-response-json)))

   ;; TRANSIT
   (POST "/files/delete/transit" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-delete
              transit/response-transit)))



   ;; Кэшированный источник файлов для картинок
   (GET "/image/*" {{path :*} :params :as request}
        (if (realized? ix/files-root-directory)
          {:stawebuser 200
           :headers {"Cache-Control" (str "max-age=" (* 60 60 24 7)) }
           :body (clojure.java.io/file (str @ix/files-root-directory "/" path))}
          (throw (Exception. "Значение пути в переменной files-root-directory еще не задано"))))

   ;; Источник файлов
   (GET "/file/*" {{path :*} :params :as request}
        (if (realized? ix/files-root-directory)
          (clojure.java.io/file (str @ix/files-root-directory "/" path))
          (throw (Exception. "Значение пути в переменной files-root-directory еще не задано"))))

   ))



;; -----------------------------------------------------------------------------

;;**************************************************************************************************
;;* BEGIN Users reference book
;;* tag: <webusers ref book>
;;*
;;* description: Справочник управления пользователями
;;*
;;**************************************************************************************************


(defn rest-webusers-list [{{:keys [page page-size fts-query]
                            :or {page 1 page-size 10 fts-query ""}} :params}]
  (-> ix/webuser-select*
      (ix/com-pred-page* (dec page) page-size)

      (as-> query
          (let [fts-query (clojure.string/trim fts-query)]
            (if (empty? fts-query)
              query
              (ix/webdoc-pred-search? query fts-query))))

      (korma.core/order :id :desc)
      ix/com-exec
      ((partial map #(dissoc % :password)))))

(defn rest-webusers-find [{{id :id} :params}]
  (-> (if id
        (-> id
            ((partial ix/com-find ix/webuser))
            (dissoc :password)
            (as-> row
                (assoc row :troles-set [(ix/webrole-list)
                                        (ix/webuserwebrole-own-get-rels-set row)])))
        (-> {} ;; new
            (assoc :troles-set [(ix/webrole-list) #{}])))))

(defn rest-webusers-save [request]
  (let [{:keys [row user-roles-keys-set]} (request :params)
        row (-> row
                (as-> row
                    (if (empty? (:password row)) (dissoc row :password)
                        (update-in row [:password] creds/hash-bcrypt)))
                ((partial ix/com-save-for-id ix/webuser))
                (dissoc :password))]

    (when user-roles-keys-set
      (println (map keyword user-roles-keys-set))
      (ix/webuserwebrole-add-rels-for-keynames row (map keyword user-roles-keys-set)))
    row))

(defn rest-webusers-delete [request]
  (-> request
      :params
      :id
      ((partial ix/com-delete-for-id ix/webuser))
      ((fn [_] {:result "OK"}))))

(defn rest-webusers-change-password [request]
  (-> request
      :params
      (update-in [:password] creds/hash-bcrypt)
      (assoc :username (-> request :session :cemerick.friend/identity :current))
      ix/webuser-save-for-username))


(defn routes-webusers* [roles-set]
  (routes
   (context "/tc/rb/webusers" []


            
            ;; JSON
            (POST "/list" request
                  (friend/authorize
                   roles-set
                   (-> request
                       rest-webusers-list
                       ring.util.response/response
                       cw/error-response-json)))

            ;; TRANSIT
            (POST "/list/transit" request
                  (friend/authorize
                   roles-set
                   (-> request
                       rest-webusers-list
                       transit/response-transit)))


            
            ;; JSON
            (POST "/find" request
                  (friend/authorize
                   roles-set
                   (-> request
                       rest-webusers-find
                       ring.util.response/response
                       cw/error-response-json)))

            ;; TRANSIT
            (POST "/find/transit" request
                  (friend/authorize
                   roles-set
                   (-> request
                       rest-webusers-find
                       transit/response-transit)))


            
            ;; JSON
            (POST "/save" request
                  (friend/authorize
                   roles-set
                   (-> request
                       rest-webusers-save
                       ring.util.response/response
                       cw/error-response-json)))

            ;; TRANSIT
            (POST "/save/transit" request
                  (friend/authorize
                   roles-set
                   (-> request
                       rest-webusers-save
                       transit/response-transit)))


            
            ;; JSON
            (POST "/delete" request
                  (friend/authorize
                   roles-set
                   (-> request
                       rest-webusers-delete
                       ring.util.response/response
                       cw/error-response-json)))

            ;; TRANSIT
            (POST "/delete/transit" request
                  (friend/authorize
                   roles-set
                   (-> request
                       rest-webusers-delete
                       transit/response-transit)))


            
            ;; JSON
            (POST "/change-password" request
                  (-> request
                      rest-webusers-change-password
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/change-password/transit" request
                  (-> request
                      rest-webusers-change-password
                      transit/response-transit))


            )))

;; END Users reference book
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Spec text reference book
;;* tag: <stext ref book>
;;*
;;* description: Справочник управления пользователями
;;*
;;**************************************************************************************************

(defn routes-stext* [edit-roles-set]
  (routes
   (context "/tc/rb/stext" []

            (POST "/list" {{:keys [page page-size fts-query]
                            :or {page 1 page-size 10 fts-query ""}} :params}
                  (-> ix/stext-select*
                      (ix/com-pred-page* (dec page) page-size)

                      (as-> query
                          (let [fts-query (clojure.string/trim fts-query)]
                            (if (empty? fts-query)
                              query
                              (ix/webdoc-pred-search? query fts-query))))

                      (korma.core/order :id :desc)
                      ix/com-exec
                      ((partial map #(dissoc % :password)))
                      ring.util.response/response
                      cw/error-response-json))

            (POST "/find" {{id :id} :params}
                  (-> id
                      ((partial ix/com-find ix/stext))
                      ring.util.response/response
                      cw/error-response-json))

            (POST "/save" request
                  (friend/authorize
                   edit-roles-set
                   (-> request
                       :params
                       :row
                       ((partial ix/com-save-for-id ix/stext))
                       ring.util.response/response
                       cw/error-response-json))

                  ))))
;; END Spec text reference book
;;..................................................................................................



(defn routes-tag* [edit-roles-set]
  (routes
   (context "/tag" []

            (POST "/path-and-chailds" request
                  (do
                    ;;(Thread/sleep 100000)
                    (-> request
                        :params
                        (update-in [:id] #(if (= % 0) nil %))
                        ix/tag-get-tree-path-and-childs
                        ring.util.response/response
                        cw/error-response-json)))

            #_(GET "/tree-as-flat-groups" []
                   (ring.util.response/response
                    (ix/tag-tree-as-flat-groups)))

            ;; TAGS WORK ------------------------------------------------------


            (POST "/save" request
                  (friend/authorize
                   edit-roles-set
                   (-> request
                       :params
                       ix/tag-save
                       ring.util.response/response
                       cw/error-response-json)))

            (POST "/delete" request
                  (friend/authorize
                   edit-roles-set
                   (-> request
                       :params
                       ix/tag-delete
                       ((fn [_] {:result "OK"}))
                       ring.util.response/response
                       cw/error-response-json)))

            ;; TAGS WORK ------------------------------------------------------
            ))
  )

;;**************************************************************************************************
;;* BEGIN Products reference book
;;* tag: <webdocs ref book>
;;*
;;* description: Справочник документов
;;*
;;**************************************************************************************************

(defn routes-webdocs* [edit-roles-set
                       {:keys [webdoc-entity
                               webdoc-select*
                               webdoc-save-fn
                               webdoc-delete-fn
                               context-path
                               spec-edit-fn
                               spec-save-fn
                               rb
                               covertors-fn
                               save-file-fn-options]
                        :or {webdoc-entity ix/webdoc
                             webdoc-select* ix/webdoc-select*
                             covertors-fn (fn [webdoc-row] webdoc-row)
                             spec-edit-fn (fn [webdoc-row] webdoc-row)
                             spec-save-fn (fn [_ _] nil)
                             rb {}
                             context-path "/tc/rb/webdocs"
                             webdoc-save-fn ix/webdoc-save
                             webdoc-delete-fn ix/webdoc-delete
                             save-file-fn-options {}}
                        :as init-row}]
  (routes
   (context context-path []

            (POST "/bytag" {{:keys [tag-id page page-size fts-query]
                             :or {page 1 page-size 10 fts-query ""}} :params}
                  (do
                    (-> webdoc-select* ;;; SRC
                        (ix/webdoc-pred-search-for-the-child-tree-tags?
                         {:id (if (= tag-id 0) nil tag-id)})

                        (ix/com-pred-page* (dec page) page-size)

                        (as-> query
                            (let [fts-query (clojure.string/trim fts-query)]
                              (if (empty? fts-query)
                                query
                                (ix/webdoc-pred-search? query fts-query))))

                        (korma.core/order :id :desc)
                        ix/com-exec
                        korma.db/transaction
                        ring.util.response/response
                        cw/error-response-json)))

            (POST "/edit" {{id :id} :params}
                  (-> {:webdoc-row (if id (ix/com-find webdoc-entity id) {})}
                      spec-edit-fn
                      (assoc :rb rb)
                      ring.util.response/response
                      cw/error-response-json))

            (POST "/webdoctags-edit-table" request
                  (-> request
                      :params
                      (ix/webdoctag-tag-tree-as-flat-groups-with-patches :path)
                      ring.util.response/response
                      cw/error-response-json))

            (POST "/save" {{:keys [webdoc-row webdoctag-ids-for-updating swops] :as row} :params}
                  (friend/authorize
                   edit-roles-set
                   (cw/error-response-json
                    (let [new-webdoc-row (when webdoc-row
                                           (-> webdoc-row
                                             ;;; SPEC HANDLER
                                               ;;(update-in-if-not-nil? [:price] bigdec)
                                               ;;(update-in-if-not-nil? [:orderindex] parseLong)
                                               ;;(update-in-if-not-nil? [:showbdate] #(new java.util.Date %))
                                               ;;(update-in-if-not-nil? [:showedate] #(new java.util.Date %))
                                               covertors-fn
                                               webdoc-save-fn
                                               ))]

                      (ring.util.response/response {:result-code 0
                                                    :spec-save-fn-result (spec-save-fn row new-webdoc-row)
                                                    :webdoc-row new-webdoc-row
                                                    :webdoctag-rows (when (and new-webdoc-row
                                                                               webdoctag-ids-for-updating)
                                                                      (->> webdoctag-ids-for-updating
                                                                           (map (partial assoc {} :id))
                                                                           ((partial ix/webdoctag-update-tags new-webdoc-row))
                                                                           doall))
                                                    })))))

            (POST "/delete" request
                  (friend/authorize
                   edit-roles-set
                   (-> request
                       :params
                       webdoc-delete-fn
                       ((fn [_] {:result "OK"}))
                       ring.util.response/response
                       cw/error-response-json)))

            (POST "/images-list" request
                  (-> request
                      :params
                      (ix/files_rel-select-files-by-* :webdoc_id)
                      ix/file-pred-images*
                      ix/com-exec
                      ring.util.response/response
                      cw/error-response-json))

            (POST "/files-list" request
                  (-> request
                      :params
                      (ix/files_rel-select-files-by-* :webdoc_id)
                      (ix/file-pred-images* :not-image)
                      ix/com-exec
                      ring.util.response/response
                      cw/error-response-json))

            (context "/upload/:id" [id]
                     (multipart/wrap-multipart-params
                      (POST "/image/avatar" request
                            (friend/authorize
                             edit-roles-set
                             (ring.util.response/response
                              (let [id (parseLong id)
                                    [{path :path} _]
                                    (ix/web-file-upload
                                     (partial ix/file-upload-rel-on-o webdoc-entity :webdoc_id {:id id}
                                              (merge {:path-prefix "/image/"} save-file-fn-options))
                                     (-> request :params :file-uploader))]
                                (ix/webdoc-save {:id id :web_title_image path})
                                "OK")))))

                     (multipart/wrap-multipart-params
                      (POST "/image" request
                            (friend/authorize
                             edit-roles-set
                             (ring.util.response/response
                              (do (ix/web-file-upload
                                   (partial ix/file-upload-rel-on-o webdoc-entity :webdoc_id {:id (parseLong id)}
                                            (merge {:path-prefix "/image/"} save-file-fn-options))
                                   (-> request :params :image-uploader))
                                  "OK")))))

                     (multipart/wrap-multipart-params
                      (POST "/file" request
                            (friend/authorize
                             edit-roles-set
                             (ring.util.response/response
                              (do
                                (ix/web-file-upload
                                 (partial ix/file-upload-rel-on-o webdoc-entity :webdoc_id {:id (parseLong id)}
                                          (merge {:path-prefix "/file/"} save-file-fn-options))
                                 (-> request :params :file-uploader))
                                "OK")))))

                     )

            (POST "/files_rel/delete" {{:keys [webdoc-id file-id]} :params}
                  (friend/authorize
                   edit-roles-set
                   (-> (do
                         (ix/files_rel-delete :webdoc_id {:id file-id} {:id webdoc-id})
                         (ring.util.response/response {:result "OK"}))
                       cw/error-response-json))
                  ))
   )
  )

;; END Products reference book
;;..................................................................................................
