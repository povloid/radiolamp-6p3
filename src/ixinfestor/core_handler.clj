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

            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])

            )
  )


(defn parseLong [v] (Long/parseLong v))

(defn do-in-if-not-nil? [params path do-fn]
  (if (get-in params path) (do-fn params) params))

(defn update-in-if-not-nil? [params path f & args]
  (update-in params path
             (fn [r]
               (if r (apply (partial f r) args)))))


(defn web-file-upload [service-upload-fn uploader-params]
  (letfn [(upload-one [{:keys [size tempfile content-type filename]}]
            (service-upload-fn {:filename filename :size size :content_type content-type} tempfile))]
    (if (map? uploader-params) (upload-one uploader-params)
        (doall (map upload-one uploader-params)))))

;;**************************************************************************************************
;;* BEGIN
;;* tag: <>
;;*
;;* description:
;;*
;;**************************************************************************************************

(defn routes-ix* []
  (defroutes routes-for-ix-pathes*

    (ANY "/ix/set" request
         (cw/ix-accoc-parametr request))

    (GET "/ixinfestor" request
         ;;(friend/authorize
         ;;#{s/webrole-warehouser-1-key}
         (-> request
             cwb/page-ixcms-main
             ring.util.response/response
             (ring.util.response/header "Content-Type" "text/html; charset=utf-8"))
         ;;)
         )

    ))



;; END
;;..................................................................................................


;;------------------------------------------------------------------------------
;; BEGIN: Fule uploading routes
;; tag: <file upload routes>
;; description: Маршруты для файлового аплоадера
;;------------------------------------------------------------------------------

(defn routes-file* []
  (defroutes routes-for-file-upload*

    (multipart/wrap-multipart-params
     (POST "/files/upload" request
           (web-file-upload ix/file-upload (-> request :params :image-uploader))))

    (POST "/files/edit" request
          (do
            (println request)
            (-> request
                :params
                ix/files-save
                ring.util.response/response)))

    ;; Кэшированный источник файлов для картинок
    (GET "/image/*" {{path :*} :params :as request}
         {:stawebuser 200
          :headers {"Cache-Control" (str "max-age=" (* 60 60 24 7)) }
          :body (clojure.java.io/file (str @ix/files-root-directory "/" path))})

    ;; Источник файлов
    (GET "/file/*" {{path :*} :params :as request}
         (clojure.java.io/file (str @ix/files-root-directory "/" path)))

    ))
;; -----------------------------------------------------------------------------

;;**************************************************************************************************
;;* BEGIN Users reference book
;;* tag: <webusers ref book>
;;*
;;* description: Справочник управления пользователями
;;*
;;**************************************************************************************************

(defn routes-webusers* []
  (context "/tc/rb/webusers" []

           (GET "/list/:page/:page-size" [page page-size]
                (ring.util.response/response
                 (-> ix/webuser-select*
                     (ix/com-pred-page* (dec (Long/parseLong page)) (Long/parseLong page-size))
                     (korma.core/order :id :desc)
                     ix/com-exec
                     ((partial map #(dissoc % :password))))))


           (GET "/find/new" []   (-> {}
                                     (assoc :troles-set [(ix/webrole-list) #{}])
                                     ring.util.response/response))
           (GET "/find/:id" [id] (-> id Long/parseLong
                                     ((partial ix/com-find ix/webuser))
                                     (dissoc :password)
                                     (as-> row
                                         (assoc row :troles-set [(ix/webrole-list)
                                                                 (ix/webuserwebrole-own-get-rels-set row)]))
                                     ring.util.response/response))


           (POST "/save" request
                 (cw/error-response-json
                  (let [{:keys [row user-roles-keys-set]} (request :params)
                        row (-> row
                                ix/print-debug->>>
                                (as-> row
                                    (if (empty? (:password row)) (dissoc row :password)
                                        (update-in row [:password] creds/hash-bcrypt)))
                                ix/print-debug->>>
                                ((partial ix/com-save-for-id ix/webuser))
                                (dissoc :password))]

                    (when user-roles-keys-set
                      (println (map keyword user-roles-keys-set))
                      (ix/webuserwebrole-add-rels-for-keynames row (map keyword user-roles-keys-set)))

                    (ring.util.response/response row))))

           )
  )
;; END Users reference book
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Spec text reference book
;;* tag: <stext ref book>
;;*
;;* description: Справочник управления пользователями
;;*
;;**************************************************************************************************

(defn routes-stext* []
  (context "/tc/rb/stext" []

           (GET "/list/:page/:page-size" [page page-size]
                (ring.util.response/response
                 (-> ix/stext-select*
                     (ix/com-pred-page* (dec (Long/parseLong page)) (Long/parseLong page-size))
                     (korma.core/order :id :desc)
                     ix/com-exec
                     ((partial map #(dissoc % :password))))))

           (GET "/find/:id" [id] (-> id
                                     Long/parseLong
                                     ((partial ix/com-find ix/stext))
                                     ring.util.response/response))

           (POST "/save" request
                 (-> request
                     :params
                     :row
                     ((partial ix/com-save-for-id ix/stext))
                     ring.util.response/response
                     cw/error-response-json))

           ))
;; END Spec text reference book
;;..................................................................................................



(defn routes-tag* []
  (context "/tag" []

           (GET "/:id/path-and-chailds" [id]
                (ring.util.response/response
                 (ix/tag-get-tree-path-and-childs
                  {:id (if (= id "root") nil (Long/parseLong id))})))

           (GET "/tree-as-flat-groups" []
                (ring.util.response/response
                 (ix/tag-tree-as-flat-groups)))

           ;; TAGS WORK ------------------------------------------------------
           (POST "/save" request
                 (-> request
                     :params
                     ix/tag-save
                     ring.util.response/response
                     cw/error-response-json))

           (POST "/delete" request
                 (-> request
                     :params
                     ix/tag-delete
                     ring.util.response/response
                     cw/error-response-json))

           ;; TAGS WORK ------------------------------------------------------
           )
  )

;;**************************************************************************************************
;;* BEGIN Products reference book
;;* tag: <webdocs ref book>
;;*
;;* description: Справочник документов
;;*
;;**************************************************************************************************

(defn routes-webdocs* [{:keys [webdoc-entity
                               webdoc-select*
                               webdoc-save-fn
                               context-path
                               covertors-fn]
                        :or {webdoc-entity ix/webdoc
                             webdoc-select* ix/webdoc-select*
                             covertors-fn (fn [webdoc-row] webdoc-row)
                             context-path "/tc/rb/webdocs"
                             webdoc-save-fn ix/webdoc-save}
                        :as init-row}]

  (context context-path []

           (GET "/bytag/:id/:page/:page-size" [id page page-size]
                (-> webdoc-select* ;;; SRC
                    (ix/webdoc-pred-search-for-the-child-tree-tags*
                     {:id (if (= id "root") nil (Long/parseLong id))})
                    (ix/com-pred-page* (dec (Long/parseLong page)) (Long/parseLong page-size))
                    (korma.core/order :id :desc)
                    ix/com-exec
                    korma.db/transaction
                    ring.util.response/response
                    cw/error-response-json))

           (GET "/edit/new" [] (ring.util.response/response {}))
           (GET "/edit/:id" [id]
                ;;TODO: Попробовать сделать все в рамках одной транзакции
                (let [id (Long/parseLong id)]
                  (-> {:webdoc-row (ix/com-find webdoc-entity id)  ;;; SRC
                       :webdoctag-edit-table (ix/webdoctag-tag-tree-as-flat-groups-with-patches {:id id} :path)
                       }
                      ring.util.response/response
                      cw/error-response-json)))

           (POST "/save" {{:keys [webdoc-row webdoctag-ids-for-updating swops]} :params}
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
                                                  :webdoc-row new-webdoc-row
                                                  :webdoctag-rows (when (and new-webdoc-row
                                                                             webdoctag-ids-for-updating)
                                                                    (->> webdoctag-ids-for-updating
                                                                         (map (partial assoc {} :id))
                                                                         ((partial ix/webdoctag-update-tags new-webdoc-row))
                                                                         doall))
                                                  }))))

           (GET "/images-list" request
                (-> request :params :id parseLong
                    ((partial hash-map :id))
                    (ix/files_rel-select-files-by-* :webdoc_id)
                    ix/file-pred-images*
                    ix/com-exec
                    ring.util.response/response
                    cw/error-response-json))

           (GET "/files-list" request
                (-> request :params :id parseLong
                    ((partial hash-map :id))
                    (ix/files_rel-select-files-by-* :webdoc_id)
                    (ix/file-pred-images* :not-image)
                    ix/com-exec
                    ring.util.response/response
                    cw/error-response-json))

           (context "/upload/:id" [id]
                    (multipart/wrap-multipart-params
                     (POST "/image/avatar" request
                           (ring.util.response/response
                            (let [id (parseLong id)
                                  [{path :path} _] (web-file-upload
                                                    (partial ix/file-upload-rel-on webdoc-entity :webdoc_id {:id id})
                                                    (-> request :params :file-uploader))]
                              (ix/webdoc-save {:id id :web_title_image path})
                              "OK"))))

                    (multipart/wrap-multipart-params
                     (POST "/image" request
                           (ring.util.response/response
                            (do (web-file-upload
                                 (partial ix/file-upload-rel-on webdoc-entity :webdoc_id {:id (parseLong id)})
                                 (-> request :params :image-uploader))
                                "OK"))))

                    (multipart/wrap-multipart-params
                     (POST "/file" request
                           (ring.util.response/response
                            (do
                              (web-file-upload
                               (partial ix/file-upload-rel-on webdoc-entity :webdoc_id {:id (parseLong id)})
                               (-> request :params :file-uploader))
                              "OK"))))

                    )

           (POST "/files_rel/delete" {{:keys [webdoc-id file-id]} :params}
                 (-> (ix/files_rel-delete :webdoc_id {:id file-id} {:id webdoc-id})
                     ring.util.response/response
                     cw/error-response-json))
           )

  )

;; END Products reference book
;;..................................................................................................
