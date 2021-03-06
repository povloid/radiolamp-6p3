(ns r6p3.core-web-bootstrap
  (:use hiccup.core)
  (:use hiccup.page)
  (:use hiccup.form)
  (:use hiccup.element)
  (:use hiccup.util)

  (:use r6p3.core-web))


;;**************************************************************************************************
;;* BEGIN main tamplate
;;* tag: <bootstrap main page template>
;;*
;;* description: основной шаблон
;;*
;;**************************************************************************************************


(defn template-main
  ([body] (template-main {} body))
  ([opts body]
   (html5 {:lang "ru"}
          [:head
           [:meta {:charset "UTF-8"}]
           [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
           [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]

           [:title (:title opts "Page Title")]

           "<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->"
           "<!-- WARNING: Respond.js doesn't work if you view the page via file:// -->"
           "<!--[if lt IE 9]>"
           "<script src=\"https://oss.maxcdn.com/libs/html5shiv/3.7.2/html5shiv.js\"></script>"
           "<script src=\"https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js\"></script>"
           "<![endif]-->"

           (include-css (get opts :bootstrap.min.css "/bootstrap/css/bootstrap.min.css"))
           (include-css (get opts :bootstrap.sonis   "/bootstrap/css/sonis.css"))
           (include-js  (get opts :jquery.min.js     "/js/jquery.min.js"))


           (:header-additions opts nil)

           ]
          [:body {:data-spy "scroll" :data-target "#body-scroll-spy"}
           body
           (include-js (get opts :bootstrap.min.js "/bootstrap/js/bootstrap.min.js"))

           ;;(include-js "/bootstrap/js/bootswatch.js")

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

(defn navbar-collapse [body]
  [:div {:id :navbar-collapse
         :class "collapse navbar-collapse"} body])

(defn nav [body]
  [:div {:class "nav navbar-nav"} body])


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

(defn nav-menu-item
  ([title attrs] (nav-menu-item title nil attrs))
  ([title glyphicon-name attrs]
   [:li
    [:a (merge {:href "#"} attrs)
     (when glyphicon-name
       [:span {:class (str "glyphicon " glyphicon-name) :aria-hidden "true"}])
     " " title]]))

(defn nav-menu
  ([title attrs nav-menu-items] (nav-menu title nil attrs nav-menu-items))
  ([title glyphicon-name attrs nav-menu-items]
   [:li {:class "dropdown"}
    [:a {:class "dropdown-toggle" :aria-expanded "false", :role "button", :data-toggle "dropdown", :href "#"}
     (when glyphicon-name
       [:span {:class (str "glyphicon " glyphicon-name) :aria-hidden "true"}])
     " " title
     [:span {:class "caret"}]]
    [:ul {:class "dropdown-menu" :role "menu"}
     nav-menu-items
     ]]))


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


(defn page-sign-in
  [{:keys [login-form-caption
           button-caption] :as opts}]
  (template-main
   opts
   (list
    (include-css (get opts :signin.css "/bootstrap/css/signin.css"))
    [:div {:class "container"}
     [:form {:method "POST" :action "login" :class "form-signin" :role "form"}
      [:h2 {:class "form-signin-heading" :style "text-align: center"}
       (or login-form-caption "Please sign in")]
      [:label {:for "inputUsername" :class "sr-only"} "User"]
      [:input {:type "text" :id "inputUsername" :name "username" :class "form-control" :placeholder "User" :required true :autofocus true}]
      [:label {:for "inputPassword" :class "sr-only"} "Password"]
      [:input {:type "password" :id "inputPassword" :name "password" :class "form-control" :placeholder "Password" :required true}]
      (comment [:div {:class "checkbox"}
                [:label [:input {:type "checkbox" :value "remember-me"}] "Remember me"]])
      [:button {:class "btn btn-lg btn-primary btn-block" :type "submit"} (or button-caption "Sign in")]
      ]])))

;; END AUTH
;;..................................................................................................


;;;**************************************************************************************************
;;;* BEGIN Carusel
;;;* tag: <carusel>
;;;*
;;;* description: Карусель для изображений
;;;*
;;;**************************************************************************************************

;; Пример заполнения


(defn make-carousel
  " Формирование карусели для показа изображений
  пример вызова:
  (def carousel-list
  (list {:title       \"Заголовок 1\"
         :description \"описание заголовка 1\"
         :src         \"/images/p1.jpg\"
         :href        \"/partners\"}
        {:title       \"Заголовок 2\"
         :description \"описание заголовка 2\"
         :src         \"/images/p2.jpg\"
         :href        \"/partners\"}
        {:title       \"Заголовок 3\"
         :description \"описание заголовка 3\"
         :src         \"/images/p3.jpg\"
         :href        \"/partners\"}
        {:title       \"Заголовок 4\"
         :description \"описание заголовка 4\"
         :src         \"/images/p4.jpg\"
         :href        \"/partners\"}
        {:title       \"Заголовок 5\"
         :description \"описание заголовка 5\"
         :src         \"/images/p5.jpg\"
         :href        \"/partners\"}))"
  [carusel-list]
  [:div#myCarousel.carousel.slide.carousel-slideFullOutLeft
   {:data-ride "carousel"}
   [:ol.carousel-indicators
    (map-indexed
     (fn [idx {:keys []}]
       [:li {:class (if (= 0 idx) "active" "") :data-slide-to (str idx), :data-target "#myCarousel"}])
     carusel-list)
    ]
   [:div.carousel-inner
    (map-indexed
     (fn [idx {:keys [title description src href]}]
       [:div.item {:class (if (= 0 idx) "active" "")}
        [:img
         {:alt title,
          :src src}]
        [:div.container
           [:div.carousel-caption
            [:h2 {:style "color:#fff;"} title]
            [:p {:style "color:#fff;"} description]
            #_[:p
             [:a.btn.btn-lg.btn-opacity
              {:role "button", :href href}
              "Узнать что там"]]]]])
     carusel-list)

    [:a.left.carousel-control
     {:data-slide "prev", :href "#myCarousel"}
     [:span.glyphicon.glyphicon-chevron-left]]
    [:a.right.carousel-control
     {:data-slide "next", :href "#myCarousel"}
     [:span.glyphicon.glyphicon-chevron-right]]]])


;; <span class="glyphicon glyphicon-search" aria-hidden="true"></span>

;;; END Carusel
;;;..................................................................................................


;;;**************************************************************************************************
;;;* BEGIN Icons
;;;* tag: <icons>
;;;*
;;;* description: формирование иконок
;;;*
;;;**************************************************************************************************

(defn font-icon [css]
  [:span {:class       css
          :aria-hidden "true"}])


(defn glyphicon [iname]
  (font-icon
   (str "glyphicon glyphicon-" iname)))

;;; END Icons
;;;..................................................................................................
