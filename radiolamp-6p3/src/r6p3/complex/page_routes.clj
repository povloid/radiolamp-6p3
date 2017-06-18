;;;**************************************************************************************************
;;;* BEGIN Web layer for page
;;;* tag: <routes page>
;;;*
;;;* description: Маршруты для страница
;;;*
;;;**************************************************************************************************

(ns r6p3.complex.page-routes
  (:use compojure.core)
  ;;(:use ring.middleware.transit)
  (:use r6p3.transit)
  (:require [r6p3.transit :as transit]
            [r6p3.complex.page-web :as wb]

            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [ring.middleware.multipart-params :as multipart]))


(defn make-routes* [{:keys [uri] :as spec}]
  (routes
   (context (str uri "/page") []

            ;; TRANSIT
            (POST "/list" request
                  (-> request
                      wb/rest-page-list
                      transit/response-transit))
            ;; TRANSIT
            (POST "/find" request
                  (-> request
                      wb/rest-page-find
                      transit/response-transit))
            ;; TRANSIT
            (POST "/save" request
                  (-> request
                      wb/rest-page-save
                      transit/response-transit))
            ;; TRANSIT
            (POST "/delete" request
                  (-> request
                      wb/rest-page-delete
                      transit/response-transit))





            ;; TRANSIT
            (POST "/images-list" request
                  (-> request
                      wb/rest-page-images-list
                      transit/response-transit))

            ;; TRANSIT
            (POST "/files-list" request
                  (-> request
                      wb/rest-page-files-list
                      transit/response-transit))

            ;; TRANSIT
            (POST "/files_rel/delete" request
                   (-> request
                       wb/rest-page-files-rel-delete
                       transit/response-transit))



            (context "/upload/:id" [id]

                     (multipart/wrap-multipart-params
                      (POST "/image" request
                             (ring.util.response/response
                              (wb/rest-page-upload-image request id) )))

                     (multipart/wrap-multipart-params
                      (POST "/file" request
                             (ring.util.response/response
                              (wb/rest-page-upload-file request id)  )))
                     )

            )))
