;; Взято из https://github.com/jalehman/ring-transit/blob/master/src/ring/middleware/transit.clj
(ns ixinfestor.transit
  (:import (java.io ByteArrayOutputStream))
  (:require [ring.util.response :refer :all]
            [plumbing.core :refer [keywordize-map]]
            [cognitect.transit :as transit]))

(defn- write [x t opts]
  (let [baos (ByteArrayOutputStream.)
        w    (transit/writer baos t opts)
        _    (transit/write w x)
        ret  (.toString baos)]
    (.reset baos)
    ret))

(defn- transit-request? [request]
  (if-let [type (:content-type request)]
    (let [mtch (re-find #"^application/transit\+(json|msgpack)" type)]
      [(not (empty? mtch)) (keyword (second mtch))])))

(defn- read-transit [request {:keys [keywords? opts]}]
  (let [[res t] (transit-request? request)]
    (if res
      (if-let [body (:body request)]
        (let [rdr (transit/reader body t opts)
              f   (if keywords? keywordize-map identity)]
          (try
            [true (f (transit/read rdr))]
            (catch Exception ex
              [false nil])))))))

(def ^{:doc "The default response to return when a Transit request is malformed."}
  default-malformed-response
  {:status  400
   :headers {"Content-Type" "text/plain"}
   :body    "Malformed Transit in request body."})

(defn wrap-transit-body
  "Middleware that parses the body of Transit request maps, and replaces the :body
  key with the parsed data structure. Requests without a Transit content type are
  unaffected.
  Accepts the following options:
  :keywords?          - true if the keys of maps should be turned into keywords
  :opts               - a map of options to be passed to the transit reader
  :malformed-response - a response map to return when the JSON is malformed"
  {:arglists '([handler] [handler options])}
  [handler & [{:keys [malformed-response]
               :or {malformed-response default-malformed-response}
               :as options}]]
  (fn [request]
    (if-let [[valid? transit] (read-transit request options)]
      (if valid?
        (handler (assoc request :body transit))
        malformed-response)
      (handler request))))

(defn- assoc-transit-params [request transit]
  (let [request (assoc request :transit-params transit)]
    (if (map? transit)
      (update-in request [:params] merge transit)
      request)))

(defn wrap-transit-params
  "Middleware that parses the body of Transit requests into a map of parameters,
  which are added to the request map on the :transit-params and :params keys.
  Accepts the following options:
  :malformed-response - a response map to return when the JSON is malformed
  :opts               - a map of options to be passed to the transit reader
  Use the standard Ring middleware, ring.middleware.keyword-params, to
  convert the parameters into keywords."
  {:arglists '([handler] [handler options])}
  [handler & [{:keys [malformed-response]
               :or {malformed-response default-malformed-response}
               :as options}]]
  (fn [request]
    (if-let [[valid? transit] (read-transit request options)]
      (if valid?
        (handler (assoc-transit-params request transit))
        malformed-response)
      (handler request))))

(defn- transit-response? [response]
  (if-let [type (get-header response "Content-Type")]
    (not (empty? (re-find #"^application/transit\+(json|msgpack)" type)))))

(defn wrap-transit-response
  "Middleware that converts responses with a map or a vector for a body into a
  Transit response.
  Accepts the following options:
  :encoding - one of #{:json :json-verbose :msgpack}
  :opts     - a map of options to be passed to the transit writer"
  {:arglists '([handler] [handler options])}
  [handler & [{:as options}]]
  (let [{:keys [encoding opts] :or {encoding :json}} options]
    (assert (#{:json :json-verbose :msgpack} encoding) "The encoding must be one of #{:json :json-verbose :msgpack}.")
    (fn [request]
      (let [response (handler request)]
        (if (and
             (coll? (:body response))
             (transit-response? response))
          (let [transit-response (update-in response [:body] write encoding opts)]
            (if (contains? (:headers response) "Content-Type")
              transit-response
              (content-type transit-response (format "application/transit+%s; charset=utf-8" (name encoding)))))
          response)))))

(defmacro response-transit [& body]
  `(try
     (-> ~@body
         ring.util.response/response
         (ring.util.response/content-type "application/transit+json;charset=utf-8"))
     (catch Exception ex#
       (do
         (clojure.stacktrace/print-stack-trace ex#)
         (-> ex#
             .getMessage
             ((fn [message#] (println message#) message#))
             ((partial array-map :error))
             ring.util.response/response
             (ring.util.response/status 500)
             (ring.util.response/content-type "application/transit+json;charset=utf-8"))))))










