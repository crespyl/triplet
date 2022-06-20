(ns app.main.core
  (:require
   ["electron" :as electron :refer [app BrowserWindow dialog ipcMain]]
   ))

;; enable CSP-header
;; (.defaultSession.webRequest.onHeadersReceived
;;  electron/session (fn [details, callback]
;;                     (callback
;;                      (clj->js {
;;                                :responseHeaders
;;                                (assoc-in (js->clj details)
;;                                          ["Content-Security-Policy"]
;;                                          ["script-src self unsafe-eval"])
;;                                }))))

(def main-window (atom nil))

(defn init-browser []
  (reset! main-window (BrowserWindow.
                        (clj->js {:width 800
                                  :height 600
                                  :webPreferences
                                  {:nodeIntegration true
                                   :preload (str js/__dirname "/preload.js")}})))

  ; Path is relative to the compiled js file (main.js in our case)
  (.loadURL ^js/electron.BrowserWindow @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js/electron.BrowserWindow @main-window "closed" #(reset! main-window nil)))

(defn handle-file-open [success-fn]
  (-> (.showOpenDialog dialog)
      (.then #(success-fn %))
      (.catch #(tap> [:file-open-error %]))
      (.finally #(tap> [:file-open-finally %]))))

(defn init-handlers []
  (.on ipcMain "toMain" (fn [event args]
                          (set! *warn-on-infer* false)
                          (tap> [:main-got-event event args])
                          (let [return #(.webContents.send @main-window "fromMain" %)]
                            (case args
                              "dialog:get-file" (handle-file-open #(return %))
                              (return "unrecognized request")))
                          (set! *warn-on-infer* true))))

(defn init []
  (init-handlers)
  (init-browser))

(defn main []
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" #(init)))
