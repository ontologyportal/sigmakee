fof(addbase, axiom, ![X]:add(X,0)=X).
fof(addstep, axiom, ![X,Y]:add(X,s(Y))=s(add(X,Y))).
fof(mulbase, axiom, ![X]:mult(X,0)=0).
fof(mulstep, axiom, ![X,Y]:mult(X,s(Y))=add(X, mult(X,Y))).
