package com.agentcore.gateway.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;

// here we are going to implement tokenbucket rate limitter algorithm 

@Getter
public class TokenBucket {
    private final String apiKey;
    private final int maxTokens;
    private final double refillRatePerMs;  //60req/min ==> 0.001 tokens/ms
    private final AtomicLong milliTokens;
    private final AtomicLong lastRefillTime;
    private final int maxConcurrent;
    private final AtomicInteger currentConcurrent;
    private final AtomicLong totalRejected;

    public TokenBucket(String apiKey, int requestsPerMinute, int maxConcurrent){
        this.apiKey=apiKey;
        this.maxTokens=requestsPerMinute;
        this.refillRatePerMs=(double)requestsPerMinute/60_000.0;
        this.milliTokens=new AtomicLong((long) maxTokens*1000);
        this.lastRefillTime=new AtomicLong(System.currentTimeMillis());
        this.maxConcurrent=maxConcurrent;
        this.currentConcurrent=new AtomicInteger(0);
        this.totalRejected=new AtomicLong(0);

    }

    public synchronized boolean tryConsume(){
        refill();

        if(currentConcurrent.get()>=maxConcurrent){
            totalRejected.incrementAndGet();
            return false;

        }

        if(milliTokens.get()>=1000){
            milliTokens.addAndGet(-1000);
            currentConcurrent.incrementAndGet();
            return true;
        }

        totalRejected.incrementAndGet();
        return false;


    }

    public void release(){
        if(currentConcurrent.get()>0){
            currentConcurrent.decrementAndGet();
        }
    }

    private void refill(){
        long now=System.currentTimeMillis();
        long last=lastRefillTime.get();
        long elapsedMs=now-last;

        if(elapsedMs<=0){
            return;
        }

        long milliTokensToAdd=(long)(elapsedMs*refillRatePerMs*1000);

        if(milliTokensToAdd > 0){
            long maxMilliTokens=(long)maxTokens*1000;
            long current=milliTokens.get();
            long updated=Math.min(maxMilliTokens,current+milliTokensToAdd);
            milliTokens.set(updated);
            lastRefillTime.set(now);

        }
    }

    public int getAvailableTokens(){
        refill();
        return (int)(milliTokens.get()/1000);
    }

    public double getRetryAfterSeconds(){
        long deficit=1000-milliTokens.get();
        if(deficit<=0){
            return 0;
        }
        double milliTokensPerMs=refillRatePerMs*1000;
        return (deficit/milliTokensPerMs)/1000.0;
        
    }    

}
