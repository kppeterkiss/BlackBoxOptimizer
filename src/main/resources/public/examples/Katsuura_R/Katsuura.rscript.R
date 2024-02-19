#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)
d = 32
r = 0
for (i in 1:(length(args) )) {
        t = 0
        for (k in 1:d) {
          t = t + floor(2^k * as.numeric(args[i])) * 2^(-k)
        }
        r = r  + (1 + (i + 1) * t)
}
cat(sprintf("Katsuura %d\n", r))