(defproject compoundorwp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.csv "1.0.1"]
                 [metasoarous/oz "2.0.0-alpha2"]]
  :middleware [cider-nrepl.plugin/middleware]
  :repl-options {:init-ns compoundorwp.core})
