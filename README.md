# Revert Compoundor
:::info
This document is a work in progress, our goal is to gather feedback.
:::


Mario Romero Zavala ([mario@revert.finance](mailto:mario@revert.finance))
Bastian Kälin ([bastian@revert.finance)](mailto:bastian@revert.finance)

## Introduction

Revert Compoundor is a protocol to automate compounding of liquidity provider fees for positions in Uniswap v3. 

Unlike its predecessors, in Uniswap v3 swap fees accrued by the protocol for LPs are not continuously deposited into the pool. One consequence of this is that fees are not compounded automatically back into the position.[^1]

Manual compounding of fees is a cumbersome process composed of the following steps:

 1) Collecting fees via the NFT Manager contract.
 2) Checking the amounts of token0 and token1 required to swap if maximizing the amount of liquidity added to the position.
 3) Possibly performing the swap.
 4) Adding the tokens as liquidity to the position.

In addition, because the fees do not grow at a constant rate, knowing when to compound them requires constant monitoring of the uncollected fees.

The Revert Compoundor protocol automates this process by allocating a fixed percentage of the fees to incentivize protocol participants (compoundors) to pay for the gas costs incurred in performing the required contract interactions.

By allocating a fixed portion of the fees, the protocol incentivizes the compounding task to be performed as often as possible given the uncollected fee value and the network gas price as constraints. 

The protocol also allows for the position owner to compound their fees conveniently themselves by calling one contract function and paying for the required gas. In this use case the protocol charges no fees.

Code:
https://github.com/revert-finance/compoundor
Documentation:
https://docs.revert.finance
Website:
https://revert.finance


## Compounding Frequency


The contract interactions described in the introduction incur in gas costs that should affect the decision of when to collect and deposit a position's fees so as to maximize returns. The relevant parameters to take into account are:

 - The value of the uncollected fees
 - The gas costs incurred in compounding said fees
 - The expected rate of return the position will accrue from fees
 - The LPs expected longevity for their position

There is a possible additional consideration with regards to the divergence loss, also known as impermanent loss[^3], the position will experience. Since uncollected fees are not part of the pool, they are not traded against. Therefore it is possible that when compounding fees the divergence loss for the compounded fees is greater than the additional returns from compounding. This effect can be amplified if swapping the uncollected fees that will be added to the position to maximize increased liquidity. 

The protocol seeks to maximize the amount of liquidity compounded as often as possible. With perfect information, a strategy that maximizes the amount of liquidity compounded will not likely compound as often as possible but at optimal price points.

We assume the position owner has taken a market position in selecting the position's range and therefore cares about maximizing the returns from fees without consideration for divergence loss.

In the estimations below we assume that prices are mean-reverting to the weighted average price of the different compounding points, and therefore divergence loss on the compounding fees is negligible.


### Compoundable Positions

The increase in performance due to compounding fees will be widely different across positions, and in some cases, the gas costs of compounding the fees may not be offset by the increased fees collected due to the compounding effect for more than a year.


#### Costs of Compounding

We can estimate the costs of compounding fees without using the auto-compounder on Mainnet (25 Gwei), Polygon (50 Gwei), Optimism (0.25 Gwei), and Arbitrum (0.2 Gwei) as per the following table[^2]. Assuming separate transactions and ETH price of $1000 and MATIC price of $0.5. For Optimism and Arbitrum estimated Gwei amounts were used to avoid doing L2 gas price calculation.

| Function      | Gas cost | Mainnet | Optimism | Arbitrum | Polygon |
| ------------- | -------- | ------- | -------- | -------- | ------- |
| Collect       | 124,000  | $3.10   | $0.03    | $0.02    | $0.003  |
| Swap          | 184,523  | $4.61   | $0.05    | $0.04    | $0.004  |
| Add liquidity | 216,912  | $5.42   | $0.05    | $0.04    | $0.005  |
| Total         | 525,435  | $13.13  | $0.13    | $0.10    | $0.012  |


Likewise we can estimate the gas costs for compounding a position using the Compoundor contract on the same chains as shown below.

| Function      | Gas cost | Mainnet | Optimism | Arbitrum | Polygon |
| ------------- | -------- | ------- | -------- | -------- | ------- |
| Auto-Compound | 479,521  | $11.99  | $0.12    | $0.10    | $0.01   |



#### Estimating Benefits of Auto-compounding

We define the relevant parameters for our estimations as follows:

***P =*** Principal amount (current value of LP position)
***APR =*** annual percentage rate from fees only (fee APR)
***GASCOST =*** current gas costs of compounding fees
***PREWARD =*** fraction of compounded fees paid to the protocol
***CREWARD =*** fraction of the compounded fees paid to the account that calls the contract function and pays for the gas. This value is always equal or less than ***PREWARD***, as it defines the fraction of the protocol rewards assigned to the caller.



**Number of Yearly Compoundings**

To estimate the number of compoundings the protocol would execute for a given position we assume that auto-compounding happens when the compounder reward, which is a fraction of the uncollected fees, reaches the gas cost for executing the operation. In practice, the number of compoundings will be slightly lower because ***compoundors*** will execute the function when it is profitable.


The number of compoundings that will be executed is a function of the estimated APY, this makes calculating these values self-referential. One way to approximate the number of compoundings is to compute the APY assuming continuous compounding which gives us an upper bound, to get a lower bound we can calculate the number of compoundings possible given the non-compounded APR. In reality the real value will fall between these bounds, for the purposes of our calculations we use $n_{min}$ which is precise enough for realistic P and APR values.





$n_{min} = \frac{P \times APR \times CREWARD} {GASCOST}$


$n_{max} = \frac{P \times \max\left\{(e ^ {APR \times (1 - PREWARD)} - 1) , APR\right\} \times CREWARD} {GASCOST}$


**Estimated APY**

To calculate the projected APY for an auto-compounded position we use the formula for compound interest with the estimated number of compoundings per year, and we subtract the protocol reward from the APR.




$APY = (1 + \frac{APR \times (1 - PREWARD)}{n_{min}}) ^{{n_{min}} t} - 1$

 
**Compounding Improvement**

The expected improvement in performance for a position during a 1 year period is the difference between the APY and the APR.

$CompImprovement = APY-APR$


The below chart uses the estimated gas cost values from the table above and values of ***PREWARD=0.02***, ***CREWARD=0.01*** to estimate APY improvements, over a 1 year period, from compounding for positions of sizes 1k USD, 10k USD, 100k USD, and 1m USD. 


![](https://i.imgur.com/yoltWV7.png)






### LP Position Longevity

The amount of time an LP position will remain open and in-range is an important consideration when deciding to autocompound a position. For example, because of the relatively high gas fees on Ethereum Mainnet, autocompouding for positions of 10k USD value will only be beneficial when fee APRs are high and the positions are expected to remain open for months at least. 


![](https://i.imgur.com/w2bvDbc.png)




On Ethereum mainnet, with smaller positions sizes the break even time required for high fee APR positions can be more than a year, however, on lower gas-price chains like Polygon, Arbitrum and Optimism the difference between "large" and "small" positions is almost negligible.


![](https://i.imgur.com/hdgtdrM.png)


### Diminishing Returns

It's important to not misunderstand the benefits of increasing compounding frequency, which has diminishing returns. If we fix the **Fee APR**, assume fee growth is constant, and 0% compounding cost, we can see the effects of increasing frequency tapering off at different points. For most compoundable positions on a yearly time-frame, a weekly compounding frequency might be good enough.

![](https://i.imgur.com/LaCciaw.png)



## The Compoundor Protocol 

The compoundor protocol is designed to be trustless and with minimal governance required. 

For Uniswap v3 positions created using the NFT manager contract, owning an NFT position allows the account to perform any possible action including withdrawing liquidity. In the case of the compoundor protocol, once an NFT is transferred into the contract, only the original owner is able to interface with the NFT position manager to remove liquidity or "collect and withdraw" fees. At the same time, the contract allows any account to "collect and redeposit" fees into the position and by doing so be paid a reward that is a fixed fraction of the compounded fees.


### Protocol Roles

#### Position owners

Any account that transfers their Uniswap v3 position to the Compoundor contract.

#### Compoundors

An account that calls the ***autoCompound*** function for positions deposited in the Compounder contract. Compoundors monitor collected fees, token prices and gas price to decide when it is profitable for them to execute auto-compound operations for the fixed portion of the compounded fees they receive as a reward.

#### Contract owner

The contract owner account. It can modify four configuration parameters, and can transfer ownership of the contract to another account or contract.

***totalRewardX64 [uint64]***: Specifies the fraction of the compounded fees as a Q0.64[^4] paid to the protocol. This fee is including the compoundor reward. The value will be set at an equivalent of 0.02 upon deployment and can only be decreased, never increased.

***compounderRewardX64 [uint64]***: Reward fraction as a Q0.64 paid to the compoundor. Must always be less than totalRewardX64. Will be set upon deployment at an equivalent of 0.01.

***maxTWAPTickDifference [uint32]***: Max amount of ticks the current tick may differ from the Oracle TWAP tick to allow swaps. A check to prevent swaps during high volatility periods or possible price manipulation attacks.

***TWAPSeconds [uint32]***: How many seconds should be used to calculate TWAP.

**Protocol Rewards**

***totalRewardX64 - compounderRewardX64***

The contract owner account gets credited the protocol reward fees and is able to withdraw those balances at their discretion.



### Protocol Rules

Positions managed by the Compoundor contract can be auto-compounded by anyone. The ***autoCompound***() function withdraws uncollected fees, optionally swaps them to maximize the liquidity that can be deposited given the position range and pool state, and adds the amounts as liquidity back into the position.

The swap is only executed if the compoundor passes a ***doSwap*** parameter as true, this might maximize the value of the fees deposited into the position, and by doing so maximize the reward the compoundor receives. On the other hand, avoiding the swap results in total less gas costs for the compounder, which might allow for earlier and more frequent compoundings.

In cases where the compoundor decides to swap: 
- The swap is executed against the same pool the positions is providing liquidity for.
- The swap is executed only if the pool oracle has enough historical data bounded by a minimum of the ***TWAPSeconds*** configuration parameter.
- The swap is executed only if the current pool tick is less than ***maxTWAPTickDifference*** away from TWAP tick (calculated over ***TWAPSeconds***).


A fraction of the compounded liquidity, defined by ***totalRewardX64***, is paid as a protocol fee and to compensate the accounts that perform the auto-compounding for the gas costs incurred.

The compoundor can decide to receive the reward payment in either or both of the compounded fee tokens, and if they want to transfer the reward tokens immediately (or at a later moment to optimize gas spent).

### Self-compounding

If the ***autocompound*** function is called by the position owner there are no fees charged, and the maximum amount possible of collected fees are added as liquidity to the position.

### autoCompound() Call

The *autoCompound()* function can be called by any account, with a parameter of type ***AutoCompoundParams*** that is composed of the following values:

***tokenID [uint256]*** -  the id of the NFT for the Uniswap V3 position to auto-compound

***rewardConversion [enum { NONE, TOKEN_0, TOKEN_1 }]*** - specifies if the compoundor reward should be paid out in both tokens (in the proportion of how the fees were collected), or one of the tokens exclusively.

***withdrawReward [bool]*** - specifies if the reward tokens, and any other tokens in balance for the compoundor, should be sent to to msg.sender immediately, or remain in the contract for later collection.

***doSwap [bool]*** - specifies if the collected fees should be swapped into the proportion required to maximize the liquidity added given the position's range and current pool state. It might be optimal to avoid swaps in order to reduce gas costs which could permit more frequent compounding.

### Incentives Alignment

A compoundor's profit is determined by the difference between the compounder rewards and the gas costs of calling the autoCompound function, as such individual compoundors could be incentivized to maximize the uncollected fee growth between compoundings. Because they are competing with other accounts that could also call the contract, would-be compoundors risk losing all the available rewards on each passing block. 

Because position owners always pay a fixed fraction of the uncollected fees, determined by totalRewardX64, as a reward to the protocol. They would optimally benefit from the theoretical limit of continuous compounding.




### Swapping Collected Fees

When the compoundors pass the doSwap parameter as true, and the minimum oracle requirements for the pool are met, a swap of the collected fees will be executed so as to maximize the total value of liquidity that is deposited into the position.

We use the current pool price ***sqrtPriceX96*** and the prices derived for the position's ***tickLower*** and ***tickUpper*** to compute the target ratio of token0 and token1 required for adding liquidity given the current pool state. We then use the current amount of uncollected fees, and any token balances for the position owner account, to compute the swap required to reach the target ratio. 

If the compounder chooses to receive only ***token0*** or ***token1*** as reward, the swap amount is adjusted accordingly.


### Account Token Balances

After auto-compound operations are executed there are usually small amounts of remainder balances as a result of the swap minimally affecting the ratio between tokens in the pool, or other causes of slippage. It is also possible to auto-compound without swapping, which most likely will also result in remaining balances. These balances are stored in the contract and credited to the position's owner to be used the next time the autoCompound function is executed. The full balances are also withdrawable at any time by the position owner.

To save on gas costs, compoundors may also decide to keep their reward balances in the compoundor contracts instead of requiring erc20 token transfers on each execution.

The functions described below help manage these token balances.

```
accountBalances(account, token) view
```
Returns the accumulated balance for given account and token address.

```
withdrawBalance(token, to, amount)
```

Transfers the accumulated balance of msg.sender for a given token to a specified address.


### Keeping Track of Positions

The following events are emitted when a Uniswap v3 position is deposited or withdrawn from the Compoundor contract. These event logs can be used by *compoundors* to track positions that are available for auto-compounding.


```
event TokenDeposited(address account, uint256 tokenId);
event TokenWithdrawn(address account, address to, uint256 tokenId);
```

To maximize rewards from auto-compounding a compoundor should closely monitor gas costs on the given network and evaluate the optimal parameters for the autoCompound call considering potential reward payouts.

A frequently used pattern for "keeper" infrastructure, is to call the functions to be executed *statically* which causes no state change in the blockchain, so requires no gas spending. Doing this with the **autoCompound** function and different parameters allows a Compoundor to simulate the results of auto-compounding positions and to execute a state-changing transaction as soon as it is profitable to do so.

We have published a reference implementation of a compoundor script that uses this technique: https://github.com/revert-finance/compoundor-js

### Upgradeability 

Upgrading contracts is not a simple task if we want to optimize for trustlessness and minimized governance. As such if an updated version of the Compoundor protocol were to be published, liquidity providers would have to explicitly decide to move to the upgraded contracts.



## User Interface

![](https://i.imgur.com/DHDFuqZ.png)



## Disclaimer

This document is for general information purposes only. It does not constitute investment advice or a recommendation or solicitation to buy or sell any investment and should not be used in the evaluation of the merits of making any investment decision. It should not be relied upon for accounting, legal or tax advice or investment recommendations. This document reflects current opinions of the authors and is subject to change without being updated.



[^1]: Hayden Adams, Noah Zinsmeister, Moody Salem, River Keefer and Dan Robinson. 2021. Uniswap v3 Core. https://uniswap.org/whitepaper-v3.pdf
[^2]: Etherscan, Ethereum gas tracker. https://etherscan.io/gastracker
[^3]: Andreas A Aigner, Gurvinder Dhaliwal. 2021. UNISWAP: Impermanent Loss and Risk Profile of a Liquidity Provider https://arxiv.org/pdf/2106.14404.pdf
[^4]: Q number format: https://en.wikipedia.org/wiki/Q_(number_format)
