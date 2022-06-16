((clojurescript-mode
  (cider-preferred-build-tool . shadow-cljs)
  (cider-default-cljs-repl . shadow)))

;; cider/clojurescript-mode doesn't set the cider type correctly by default
(add-hook 'clojurescript-mode-hook (lambda ()
                                     (setq cider-repl-type 'cljs)))
