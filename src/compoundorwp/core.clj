(ns compoundorwp.core
  (:require
   [clojure.string :as string]
   [clojure.edn :as edn]
   [oz.core :as oz]))

(def compounding-gas-costs
  {:mainnet 11.99M
   :optimism 0.12M
   :arbitrum 0.1M
   :polygon 0.01M})

;; fraction of compounded fees
(def compoundor-reward 0.01M)

(defn num-compounds
  [principal apr compounder-fees gas-costs]
  (/ (* principal (* apr compounder-fees))
     gas-costs))

(defn num-compounds-max
  [principal apr compounder-fees gas-costs]
  (/ (* principal (* (- (Math/exp apr) 1M) compounder-fees))
     gas-costs))

(defn estimate-apy
  [apr compounder-fees n t]
  (let [apr' (* apr (- 1M compounder-fees))
        x (+ 1 (/ apr' n))
        y (* n t)]
    (Math/pow x y)))

(defn estimate-autocomp
  [position-value gas-costs fee-apr]
  (let [n (num-compounds
           position-value fee-apr compoundor-reward gas-costs)
        apy (- (estimate-apy fee-apr compoundor-reward n 1M) 1M)]
    [apy n fee-apr]))



(defn autocomp-data
  [size gas-cost chain order]
  (->> (map #(estimate-autocomp size gas-cost %)
            (range 0.1 1.5 0.01))
       (map (fn [[apy n fee-apr]]
              {:compounded-apy (* 100M apy)
               :n n
               :order order
               :chain chain
               :apy-diff (* 100M (- apy fee-apr))
               :apr (* 100M fee-apr)
               :size (str size)}))))


;; Renders Fee APR vs Comp improvement for 1k, 10k, 100k, 1m
;; for every chain
#_(oz/view!
  {:width 300
   :height 250
   :data {:values
          (concat (autocomp-data 1000M (:mainnet compounding-gas-costs) "mainnet" 0)
                  (autocomp-data 10000M (:mainnet compounding-gas-costs) "mainnet" 0)
                  (autocomp-data 100000M (:mainnet compounding-gas-costs) "mainnet" 0)
                  (autocomp-data 1000000M (:mainnet compounding-gas-costs) "mainnet" 0)
                  ;;
                  (autocomp-data 1000M (:optimism compounding-gas-costs) "optimism" 1)
                  (autocomp-data 10000M (:optimism compounding-gas-costs) "optimism" 1)
                  (autocomp-data 100000M (:optimism compounding-gas-costs) "optimism" 1)
                  (autocomp-data 1000000M (:optimism compounding-gas-costs) "optimism" 2)
                  ;;
                  (autocomp-data 1000M (:arbitrum compounding-gas-costs) "arbitrum" 2)
                  (autocomp-data 10000M (:arbitrum compounding-gas-costs) "arbitrum" 2)
                  (autocomp-data 100000M (:arbitrum compounding-gas-costs) "arbitrum" 2)
                  (autocomp-data 1000000M (:arbitrum compounding-gas-costs) "arbitrum" 2)
                  ;;
                  (autocomp-data 1000M (:polygon compounding-gas-costs) "polygon" 3)
                  (autocomp-data 10000M (:polygon compounding-gas-costs) "polygon" 3)
                  (autocomp-data 100000M (:polygon compounding-gas-costs) "polygon" 3)
                  (autocomp-data 1000000M (:polygon compounding-gas-costs) "polygon" 3))}
   :encoding {:facet {:field "chain"
                      :type "nominal"
                      :title "Chain"
                      :header {:labelAngle 0
                               :labelPadding -10
                               :labelBaseline "line-bottom"}
                      :sort {:op "median" :field "order"}
                      :columns 2}
              :x {:field "apr" :type "quantitative"
                  :title "Fee APR %"}
              :y {:field "apy-diff" :type "quantitative"
                  :title "Compounding Improvement %"}
              :yd2 {:field "n" :type "quantitative"
                   :title "Compoundings"}
              :color {:field "size" :type "nominal"
                      :title "Position Size"}}
   :mark "line"
   :xresolve {:scale {:y "independent"}}})



(defn estimate-n-comps
  [position-value gas-costs fee-apr]
  (let [n (num-compounds
           position-value fee-apr compoundor-reward gas-costs)
        apy (if (= (int n) 0)
                0 (- (estimate-apy fee-apr compoundor-reward n 1M) 1M))]
    [apy n fee-apr]))


(defn n-autocomp-data
  [size gas-cost chain order]
  (->> (map #(estimate-n-comps size gas-cost %)
            (range 0.0 1.5 0.01))
       (map (fn [[apy n fee-apr]]
              {:compounded-apy (* 100M apy)
               :n n
               :order order
               :chain chain
               :apy-diff (* 100M (- apy fee-apr))
               :apr (* 100M fee-apr)
               :size (str size)}))))



#_(oz/view!
  {:width 300
   :height 250
   :data {:values
          (concat
           (n-autocomp-data 1000M (:mainnet compounding-gas-costs) "mainnet" 0)
           (n-autocomp-data 10000M (:mainnet compounding-gas-costs) "mainnet" 0)
           (n-autocomp-data 100000M (:mainnet compounding-gas-costs) "mainnet" 0)
           (n-autocomp-data 1000000M (:mainnet compounding-gas-costs) "mainnet" 0)
           ;;
           (n-autocomp-data 1000M (:optimism compounding-gas-costs) "optimism" 1)
           (n-autocomp-data 10000M (:optimism compounding-gas-costs) "optimism" 1)
           (n-autocomp-data 100000M (:optimism compounding-gas-costs) "optimism" 1)
           (n-autocomp-data 1000000M (:optimism compounding-gas-costs) "optimism" 2)
           ;;
           (n-autocomp-data 1000M (:arbitrum compounding-gas-costs) "arbitrum" 2)
           (n-autocomp-data 10000M (:arbitrum compounding-gas-costs) "arbitrum" 2)
           (n-autocomp-data 100000M (:arbitrum compounding-gas-costs) "arbitrum" 2)
           (n-autocomp-data 1000000M (:arbitrum compounding-gas-costs) "arbitrum" 2)
           ;;
           (n-autocomp-data 1000M (:polygon compounding-gas-costs) "polygon" 3)
           (n-autocomp-data 10000M (:polygon compounding-gas-costs) "polygon" 3)
           (n-autocomp-data 100000M (:polygon compounding-gas-costs) "polygon" 3)
           (n-autocomp-data 1000000M (:polygon compounding-gas-costs) "polygon" 3))}
   :encoding {:facet {:field "chain"
                      :type "nominal"
                      :title "Chain"
                      :header {:labelAngle 0
                               :labelPadding -10
                               :labelBaseline "line-bottom"}
                      :sort {:op "median" :field "order"}
                      :columns 2}
              :x {:field "apr" :type "quantitative"
                  :title "Fee APR %"
                  :scale  {:domain [0, 140]}}
              :y {:field "n" :type "quantitative"
                   :scale {:domain [0, 100]
                           :clamp true}
                   :title "Nmin compoundings"}
              :color {:field "size" :type "nominal"
                      :title "Position Size"}}
   :mark {:type "line"
          :clip true}
   :resolvex {:scale {:y "independent"}}})




(defn fixed-apr-estimate-n-comps
  [fee-apr n]
  (let [compoundor-reward 0.00M ;; asumme 0 compounding costs
        apy  (- (estimate-apy fee-apr compoundor-reward n 1M) 1M)]
    [apy n fee-apr]))


(defn fixed-apr-autocomp-data
  [fee-apr]
  (->> (map #(fixed-apr-estimate-n-comps fee-apr %)
            (range 1 1000 1))
       (map (fn [[apy n fee-apr]]
              {:compounded-apy (* 100M apy)
               :n n
               :apy-diff (* 100M (- apy fee-apr))
               :apr (* 100M fee-apr)}))))


;; Compoduning improvement vs n-compoundings
#_(oz/view!
  {:width 600
   :height 500
   :data {:values
          (concat (fixed-apr-autocomp-data 0.6)
                  (fixed-apr-autocomp-data 0.8)
                  (fixed-apr-autocomp-data 1.0)
                  (fixed-apr-autocomp-data 1.2))}
   :title "Compounding improvement after 1 year"
   :encoding {:facetx {:field "chain"
                      :type "nominal"
                      :title "Chain"
                      :header {:labelAngle 0
                               :labelPadding -10
                               :labelBaseline "line-bottom"}
                      :sort {:op "median" :field "order"}
                      :columns 2}
              :y {:field "apy-diff" :type "quantitative"
                  :title "Compounding Improvement %"
                  :scale  {:domain [0, 120]}}
              :x {:field "n" :type "quantitative"
                   :scale {:domain [1, 200]
                           :clamp true}
                   :title "n compoundings"}
              :color {:field "apr" :type "nominal"
                      :title "Fee APR %"}}
   :mark {:type "line"
          :clip true}
   :resolvex {:scale {:y "independent"}}})



(defn estimated-value-compounded
  [pos-value apr reward days gas-costs]
  (with-precision 10
    (let [n-comps (num-compounds pos-value apr reward gas-costs)
          year-portion (/ days 365M)
          apr' (* apr (- 1M reward))
          x (+ 1M (/ apr' n-comps))
          y (* n-comps year-portion)
          apy (estimate-apy pos-value reward n-comps year-portion)]
      (* pos-value (Math/pow x y)))))



(defn estimated-value
  [pos-value apr days]
  (with-precision 10
    (+ pos-value
       (* (/ (* pos-value apr)
             365M)
          days))))


(defn compoundings-diff
  [pos-value apr gas-costs days]
  (let [reward 0.01M
        no-comp (estimated-value pos-value apr days)
        with-comp (estimated-value-compounded
                   pos-value apr reward days gas-costs)
        comp-improvement (- with-comp no-comp)]
    [no-comp with-comp comp-improvement days]))



(defn improvement-diff
  [pos-value no-compound with-compound]
  (let [wcomps (/ with-compound pos-value)
        ncomps (/ no-compound pos-value)]
    (* 100 (- wcomps ncomps))))


(defn compounding-diffs-dailys
  [pos-value apr gas-costs chain order]
  (->> (map #(compoundings-diff pos-value apr gas-costs %)
            (range 366))
       (map (fn [[no-comp with-comp comp-diff days]]
              {:days days
               :chain chain
               :order order
               :fee-apr apr
               :fee-apr-pct (str (int (* apr 100)) "%")
               :size (str pos-value)
               :no-comp no-comp
               :comp-improvement (improvement-diff pos-value no-comp with-comp)
               :with-comp with-comp
               :comp-diff comp-diff}))))







;; Compoding Improvement by n-days for each chain
;; fixed position sizes
#_(let [size 1000M]
    (oz/view!
     {:width 300
      :height 250
      :data {:values
             (concat
              (compounding-diffs-dailys size 0.2M (:mainnet compounding-gas-costs) "mainnet" 0)
              (compounding-diffs-dailys size 0.4M (:mainnet compounding-gas-costs) "mainnet" 0)
              (compounding-diffs-dailys size 0.6M (:mainnet compounding-gas-costs) "mainnet" 0)
              (compounding-diffs-dailys size 0.8M (:mainnet compounding-gas-costs) "mainnet" 0)
                             ;;
              (compounding-diffs-dailys size 0.2M (:optimism compounding-gas-costs) "optimism" 1)
              (compounding-diffs-dailys size 0.4M (:optimism compounding-gas-costs) "optimism" 1)
              (compounding-diffs-dailys size 0.6M (:optimism compounding-gas-costs) "optimism" 1)
              (compounding-diffs-dailys size 0.8M (:optimism compounding-gas-costs) "optimism" 2)
              ;;
              (compounding-diffs-dailys size 0.2M (:arbitrum compounding-gas-costs) "arbitrum" 2)
              (compounding-diffs-dailys size 0.4M (:arbitrum compounding-gas-costs) "arbitrum" 2)
              (compounding-diffs-dailys size 0.6M (:arbitrum compounding-gas-costs) "arbitrum" 2)
              (compounding-diffs-dailys size 0.8M (:arbitrum compounding-gas-costs) "arbitrum" 2)
              ;;
              (compounding-diffs-dailys size 0.2M (:polygon compounding-gas-costs) "polygon" 3)
              (compounding-diffs-dailys size 0.4M (:polygon compounding-gas-costs) "polygon" 3)
              (compounding-diffs-dailys size 0.6M (:polygon compounding-gas-costs) "polygon" 3)
              (compounding-diffs-dailys size 0.8M (:polygon compounding-gas-costs) "polygon" 3))}
      :encoding {:facet {:field "chain"
                                        :title "Position Size: 1k USD"
                         :header {:labelAngle 0
                                  :labelPadding -10
                                  :labelBaseline "line-bottom"}
                         :type "nominal"
                         :sort {:op "median" :field "order"}
                         :columns 2}
                 :x {:field "days" :type "quantitative"
                     :scale {:domain [0, 365]}
                     :title "days"}
                 :y {:field "comp-improvement" :type "quantitative"
                     :title "Compounding Improvement %"}
                 :color {:field "fee-apr-pct" :type "nominal"
                         :title "Fee APR"}}
      :mark "line"}))





(defn simulate-compoundor
  [pos-value uncollected-fees gas-costs]
  (let [creward 0.01M
        preward 0.02M
        reward-bounty (* creward uncollected-fees)
        compound? (>= (* reward-bounty uncollected-fees) gas-costs)
        pos-value' (if compound?
                    (+ pos-value (* uncollected-fees (- 1M preward)))
                    pos-value)
        uncollected-fees' (if compound? 0M uncollected-fees)]
    {:pos-value pos-value'
     :compound? compound?
     :total-value (+ pos-value' uncollected-fees')
     :uncollected-fees uncollected-fees'}))




(defn simulate-compoundable-minutes
  [pos-states apr gas-costs minute]
  (let [pos-state (last pos-states)]
    (if (= minute (* 365 24 60))
      pos-states
      (let [minute-apr (/ apr (* 365 24 60))
            pos-value (:pos-value pos-state)
            uncollected-fees (:uncollected-fees pos-state)
            init-pos-value (:pos-value (first pos-states))
            minute-fees (* pos-value minute-apr)
            uncollected-fees' (+ minute-fees uncollected-fees)
            compoundable-minute (simulate-compoundor pos-value uncollected-fees' gas-costs)
            roi (/ pos-value init-pos-value)]
        (recur (conj pos-states
                     (into compoundable-minute
                           {:minute minute
                            :roi roi}))
               apr gas-costs (+ minute 1))))))



#_(time (def minutes (simulate-compoundable-minutes
                      [{:pos-value 10000 :uncollected-fees 0M}] 0.8 0.1 1)))

#_(oz/view!
  {:width 900
   :height 450
   :data {:values minutes}
   :encoding {
              :x {:field "minute" :type "quantitative"
                  :title "minutes"}
              :y {:field "roi" :type "quantitative"
                  :title "roi"}
              :colorx {:field "size" :type "nominal"
                      :title "Position Size"}}
   :mark "line"})






