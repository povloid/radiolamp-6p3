(ns ixinfestor.omut-google-editor
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [goog.dom :as gdom]
            [goog.editor.Field :as goog-editor-field]
            [goog.editor.plugins.BasicTextFormatter :as basic-text-formatter]
            [goog.editor.plugins.RemoveFormatting :as remove-formatting]
            [goog.editor.plugins.UndoRedo :as undo-redo]
            [goog.editor.plugins.ListTabHandler :as list-tab-handler]
            [goog.editor.plugins.SpacesTabHandler :as spaces-tab-handler]
            [goog.editor.plugins.EnterHandler :as enter-handler]
            [goog.editor.plugins.HeaderFormatter :as header-formatter]
            [goog.editor.plugins.LinkDialogPlugin :as link-dialog-plugin]
            [goog.editor.plugins.LinkBubble :as link-bubble]
            [goog.ui.editor.DefaultToolbar :as default-toolbar]
            [goog.ui.editor.ToolbarController :as toolbar-controller]

            ))

(def goog-editor-app-init
  {:value ""
   })


(defonce modals-ids (atom 0))
(defn- get-goog-editor-id [] (str "goog-editor-" (swap! modals-ids inc)))


(defn goog-editor [app owner _]
  (reify
    om/IInitState
    (init-state [_]
      (let [id (get-goog-editor-id)
            id-toolbar (str id "-toolbar")]
        {:id id
         :id-toolbar id-toolbar
         :goog-editor nil}))
    om/IDidMount
    (did-mount [_]
      (let [{:keys [id id-toolbar]} (om/get-state owner)
            goog-editor (goog.editor.Field. id)
            buttons [goog.editor.Command.BOLD,
                     goog.editor.Command.ITALIC,
                     goog.editor.Command.UNDERLINE,
                     goog.editor.Command.FONT_COLOR,
                     goog.editor.Command.BACKGROUND_COLOR,
                     goog.editor.Command.FONT_FACE,
                     goog.editor.Command.FONT_SIZE,
                     goog.editor.Command.LINK,
                     goog.editor.Command.UNDO,
                     goog.editor.Command.REDO,
                     goog.editor.Command.UNORDERED_LIST,
                     goog.editor.Command.ORDERED_LIST,
                     goog.editor.Command.INDENT,
                     goog.editor.Command.OUTDENT,
                     goog.editor.Command.JUSTIFY_LEFT,
                     goog.editor.Command.JUSTIFY_CENTER,
                     goog.editor.Command.JUSTIFY_RIGHT,
                     goog.editor.Command.SUBSCRIPT,
                     goog.editor.Command.SUPERSCRIPT,
                     goog.editor.Command.STRIKE_THROUGH,
                     goog.editor.Command.REMOVE_FORMAT]]



        (goog.ui.editor.ToolbarController.
         goog-editor
         (goog.ui.editor.DefaultToolbar.makeToolbar.
          (clj->js buttons)
          (goog.dom.getElement. id-toolbar)))


        (.registerPlugin goog-editor (new goog.editor.plugins.BasicTextFormatter))
        (.registerPlugin goog-editor (new goog.editor.plugins.RemoveFormatting))
        (.registerPlugin goog-editor (new goog.editor.plugins.UndoRedo))
        (.registerPlugin goog-editor (new goog.editor.plugins.ListTabHandler))
        (.registerPlugin goog-editor (new goog.editor.plugins.SpacesTabHandler))
        (.registerPlugin goog-editor (new goog.editor.plugins.EnterHandler))
        (.registerPlugin goog-editor (new goog.editor.plugins.HeaderFormatter))
        (.registerPlugin goog-editor (new goog.editor.plugins.LinkDialogPlugin))
        (.registerPlugin goog-editor (new goog.editor.plugins.LinkBubble))



        (. goog-editor (makeEditable))

        (om/set-state! owner :goog-editor goog-editor)
        ))
    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys [goog-editor]} (om/get-state owner)]
        (. goog-editor (makeUneditable))
        ))
    om/IRenderState
    (render-state [_ {:keys [id id-toolbar]}]
      (dom/div nil
               (dom/div #js {:id id-toolbar
                             :style #js {:width 602}})
               (dom/div #js {:id id
                             :style #js {:width 600
                                         :height 300}})
               ))))
