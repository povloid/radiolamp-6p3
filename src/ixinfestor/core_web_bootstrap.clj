(ns ixinfestor.core-web-bootstrap
  (:use hiccup.core)
  (:use hiccup.page)
  (:use hiccup.form)
  (:use hiccup.element)
  (:use hiccup.util)

  (:use ixinfestor.core-web))


;;**************************************************************************************************
;;* BEGIN main tamplate
;;* tag: <bootstrap main page template>
;;*
;;* description: основной шаблон
;;*
;;**************************************************************************************************


(defn template-main
  ([body] (template-main {} body))
  ([page-params body]
   (html5 {:lang "ru"}
          [:head
           [:meta {:charset "UTF-8"}]
           [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
           [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
           [:title (:title page-params "Page Title")]

           "<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->"
           "<!-- WARNING: Respond.js doesn't work if you view the page via file:// -->"
           "<!--[if lt IE 9]>"
           "<script src=\"https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js\"></script>"
           "<script src=\"https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js\"></script>"
           "<![endif]-->"

           (include-js "/js/jquery-2.1.3.min.js")
           (include-css "/bootstrap/css/bootstrap.min.css")
           (include-css "/bootstrap/css/bootswatch.min.css")
           (include-css "/bootstrap/css/sonis.css")
           ;;(include-css "/bootstrap/css/bootstrap-theme.min.css")

           (include-js "/bootstrap/js/bootstrap.min.js")

           (:header-additions page-params nil)
           ]
          [:body
           body
           ;;(reduce conj [:div {:id "root"}] body)
           ;;(include-js "/js/tarsonis.js")
           ;;(javascript-tag "hello.hello('main.core');")
           ])))



;; END main tamplate
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN container
;;* tag: <bootstrap container>
;;*
;;* description: контенеры
;;*
;;**************************************************************************************************

(defn container
  ([body] (container {} body))
  ([attrs body]
   [:div (merge {:class "container"} attrs) body]))

(defn container-fluid
  ([body] (container-fluid {} body))
  ([attrs body]
   [:div (merge {:class "container-fluid"} attrs) body]))

;; END container
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN navbar
;;* tag: <bootstrap navbar>
;;*
;;* description: панель навигации
;;*
;;**************************************************************************************************

(def navbar-class "navbar")
(def navbar-class-default "navbar-default")
(def navbar-class-fixed-bottom "navbar-fixed-bottom")
(def navbar-class-static-top "navbar-static-top")
(def navbar-class-inverse "navbar-inverse")

(defn navbar
  ([body] (navbar nil body))
  ([plus-class-params body]
   [:nav#main-navbar {:class (reduce #(str %1 " " %2) "navbar" (or plus-class-params ""))}
    body]))


(defn navbar-header [href title]
  [:div.navbar-header
   [:button.navbar-toggle
    {:type "button" :class "navbar-toggle collapsed" :data-toggle "collapse"
     :data-target "#navbar" :aria-expanded "false" :aria-controls "navbar"}
    [:span.sr-only "Toggle navigation"]
    [:span.icon-bar]
    [:span.icon-bar]
    [:span.icon-bar]]
   [:a.navbar-brand {:href href} title]])

;; END navbar
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Modal dialogs
;;* tag: <modal dialogs>
;;*
;;* description: Модальные диалоги
;;*
;;**************************************************************************************************

(defn modal [{:keys [id label header title body footer]}]
  [:div.modal
   {:id   id :aria-hidden "true", :aria-labelledby (or label "Modal Label..."),
    :role "dialog", :tabindex "-1"}
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header
      (or header [:h4.modal-title (or title "Modal title...")])]
     [:div.modal-body (or body nil)]
     [:div.modal-footer
      [:div.navbar-left]
      (or footer [:button.btn.btn-default {:type "button" :data-dismiss "modal"} "Закрыть"])
      ]]]])

;; END Modal dialogs
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN AUTH
;;* tag: <auth>
;;*
;;* description: Аутентификация пользователя
;;*
;;**************************************************************************************************

(def login-form
  [:div {:class "row"}
   [:div {:class "columns small-12"}
    [:h3 "Login"]
    [:div {:class "row"}
     [:form {:method "POST" :action "login" :class "columns small-4"}
      [:div "Username" [:input {:type "text" :name "username"}]]
      [:div "Password" [:input {:type "password" :name "password"}]]
      [:div [:input {:type "submit" :class "button" :value "Login"}]]]]]])


(def page-sign-in
  (template-main
   (list
    (include-css "/bootstrap/css/signin.css")
    [:div {:class "container"}
     [:form {:method "POST" :action "login" :class "form-signin" :role "form"}
      [:h2 {:class "form-signin-heading"} "Please sign in"]
      [:label {:for "inputUsername" :class "sr-only"} "User"]
      [:input {:type "text" :id "inputUsername" :name "username" :class "form-control" :placeholder "User" :required true :autofocus true}]
      [:label {:for "inputPassword" :class "sr-only"} "Password"]
      [:input {:type "password" :id "inputPassword" :name "password" :class "form-control" :placeholder "Password" :required true}]
      (comment [:div {:class "checkbox"}
                [:label [:input {:type "checkbox" :value "remember-me"}] "Remember me"]])
      [:button {:class "btn btn-lg btn-primary btn-block" :type "submit"} "Sign in"]
      ]])))

;; END AUTH
;;..................................................................................................
