;;;**************************************************************************************************
;;;* BEGIN Web layer for scontent
;;;* tag: <routes scontent>
;;;*
;;;* description: Маршруты для контент для сайта
;;;*
;;;**************************************************************************************************

(ns r6p3.complex.scontent-routes
  (:use compojure.core)
  ;;(:use ring.middleware.transit)
  (:use r6p3.transit)
  (:require [r6p3.transit :as transit]
            [r6p3.complex.scontent-web :as wb]

            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [ring.middleware.multipart-params :as multipart]))


(defn make-routes* [{:keys [uri] :as spec}]
  (routes
   (context (str uri "/scontent") []

            ;; TRANSIT
            (POST "/list" request
                  (-> request
                      wb/rest-scontent-list
                      transit/response-transit))
            ;; TRANSIT
            (POST "/find" request
                  (-> request
                      wb/rest-scontent-find
                      transit/response-transit))
            ;; TRANSIT
            (POST "/save" request
                  (-> request
                      wb/rest-scontent-save
                      transit/response-transit))
            ;; TRANSIT
            (POST "/delete" request
                  (-> request
                      wb/rest-scontent-delete
                      transit/response-transit))





            ;; TRANSIT
            (POST "/images-list" request
                  (-> request
                      wb/rest-scontent-images-list
                      transit/response-transit))

            ;; TRANSIT
            (POST "/files-list" request
                  (-> request
                      wb/rest-scontent-files-list
                      transit/response-transit))

            ;; TRANSIT
            (POST "/files_rel/delete" request
                  (-> request
                      wb/rest-scontent-files-rel-delete
                      transit/response-transit))



            (context "/upload/:id" [id]

                     (multipart/wrap-multipart-params
                      (POST "/image" request
                            (ring.util.response/response
                             (wb/rest-scontent-upload-image request id) )))

                     (multipart/wrap-multipart-params
                      (POST "/file" request
                            (ring.util.response/response
                             (wb/rest-scontent-upload-file request id)  )))
                     )

            )))
