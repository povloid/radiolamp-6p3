;;;**************************************************************************************************
;;;* BEGIN Web layer for webusers
;;;* tag: <routes webusers>
;;;*
;;;* description: Маршруты для управления пользователями
;;;*
;;;**************************************************************************************************

(ns r6p3.complex.webusers-routes
  (:use compojure.core)
  (:use r6p3.transit)
  (:require [r6p3.transit :as transit]
            [r6p3.core-web :as cw]
            [r6p3.complex.webusers-web :as cww]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))


(defn routes-webusers* [{:keys [view-role
                                edit-role]
                         :as opts}]
  (routes
   (context "/tc/rb/webusers" []

            ;; JSON
            (POST "/list" request
                  (-> request
                      (cww/rest-webusers-list opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/list/transit" request
                  (-> request
                      (cww/rest-webusers-list opts)
                      transit/response-transit))



            ;; JSON
            (POST "/find" request
                  (-> request
                      (cww/rest-webusers-find opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/find/transit" request
                  (-> request
                      (cww/rest-webusers-find opts)
                      transit/response-transit))



            ;; JSON
            (POST "/save" request
                  (-> request
                      (cww/rest-webusers-save opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/save/transit" request
                  (-> request
                      (cww/rest-webusers-save opts)
                      transit/response-transit))



            ;; JSON
            (POST "/delete" request
                  (-> request
                      (cww/rest-webusers-delete opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/delete/transit" request
                  (-> request
                      (cww/rest-webusers-delete opts)
                      transit/response-transit))



            ;; JSON
            (POST "/change-password" request
                  (-> request
                      cww/rest-webusers-change-password
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/change-password/transit" request
                  (-> request
                      cww/rest-webusers-change-password
                      transit/response-transit))


            )))
