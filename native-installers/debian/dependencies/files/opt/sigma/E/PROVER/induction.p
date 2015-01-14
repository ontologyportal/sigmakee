fof(app0, axiom, ![X]:append(nil, X) = X).
fof(appcons, axiom, ![X,Y,Z]:append(cons(X,Y),Z) = cons(X, append(Y,Z))).

#fof(len0, axiom, len(nil)=0).
#fof(len0, axiom, len(cons(X,Y)) = s(len(Y))).

    
# fof(list1, axiom, l1 = cons(a, cons(b, cons(c, nil)))).
# fof(list2, axiom, l2 = cons(d, cons(e, cons(f, nil)))).


# fof(test1, conjecture, append(l1, l2) = cons(a, cons(b, cons(c, cons(d, cons(e, cons(f, nil))))))).
# fof(test2, question, ?[X]:(X = append(l1,l2)&len(X)=s(s(s(s(s(s(0)))))))).
# fof(test2, conjecture, len(append(l1,l2))=s(s(s(s(s(s(0))))))).

fof(induction, axiom,
    ((![X,Y,Z]:append(nil, append(Y,Z)) = append(append(nil, Y),Z)
    &
    ![X,XR,Y,Z]:append(cons(X,XR), append(Y,Z)) = append(append(cons(X,XR),Y),Z))
    =>
    ![X,Y,Z]:append(X, append(Y,Z)) = append(append(X,Y),Z))).


fof(induction2, axiom,
    ((![X,Y,Z]:append(X, append(Y,nil)) = append(append(X, Y),nil)
    &
    ![X,XR,Y,Z]:append(X, append(Y,cons(Z, ZR))) = append(append(X,Y),cons(Z,ZR)))
    =>
    ![X,Y,Z]:append(X, append(Y,Z)) = append(append(X,Y),Z))).

fof(appisass, conjecture, ![X,Y,Z]:append(X, append(Y,Z)) = append(append(X,Y),Z)).
