fof(socrates,axiom,(philosopher(socrates))).
fof(plato,axiom,(philosopher(plato))).
fof(hume,axiom,(philosopher(hume))).

fof(philosphers_exist,question,( ? [X,Y] : (philosopher(X)&philosopher(Y)))).

