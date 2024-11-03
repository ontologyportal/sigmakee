# Contributing

By participating in this project, you agree to abide by the [code of conduct](#code-of-conduct).

## Issues

One of the easiest ways to contribute is to solve an issue https://github.com/ontologyportal/sigmakee/issues

## Additions

* We use jUnit for testing.  When making any addition of functionality, please add some jUnit tests to
show that it works and prevent regressions.
* adhere to the Java style guide for Sigma - https://github.com/ontologyportal/sigmakee/blob/master/CodeFormat.pdf
* if the functionality requires a GUI, add a .jsp interface copying and modifying an existing JSP in Sigma
* if the functionality can be meaningfully executed on the command line, add
to the main() for the class, following the style of an existing class like com.articulate.sigma.KB
with its showHelp() method so it functions like a standard Unix command with a man page


## Pull Requests

We love pull requests from everyone.

https://git-scm.com/book/en/v2/GitHub-Contributing-to-a-Project

Some things that will increase the chance that your pull request is accepted:

* Make small incremental changes.
* Avoid the use of automatic tools or formatters to keep commits small and trackable.
* Write a [good commit message][commit].
* explain what your addition attempts to accomplish
* provide the transcript showing that you've run all the existing jUnit tests for Sigma (as specified
in the README https://github.com/ontologyportal/sigmakee/blob/master/README.txt) and that they pass

[commit]: http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html

## Code of conduct

In order to foster an inclusive, kind, harassment-free, and cooperative community,
we enforce this code of conduct on our open source projects.

### Summary

Harassment in code and discussion or violation of physical boundaries is completely
unacceptable anywhere in our project. Violators will be warned by the core team.
Repeat violations will result in being blocked or banned by the core team at or
before the 3rd violation.

### In detail

Harassment includes offensive verbal comments related to gender identity,
gender expression, sexual orientation, disability, physical appearance, body size,
race, religion, sexual images, deliberate intimidation, stalking, sustained
disruption, and unwelcome sexual attention.

Individuals asked to stop any harassing behavior are expected to comply immediately.

Maintainers are also subject to the anti-harassment policy.

If anyone engages in harassing behavior, including maintainers, we may take
appropriate action, up to and including warning the offender, deletion of
comments, removal from the project’s codebase and communication systems,
and escalation to GitHub support.

If you are being harassed, notice that someone else is being harassed, or
have any other concerns, please contact us immediately

We expect everyone to follow these rules anywhere in our project.

Finally, don't forget that it is human to make mistakes! We all do. Let’s
work together to help each other, resolve issues, and learn from the mistakes
that we will all inevitably make from time to time.

### Thanks

Derived from [thoughbot's code of conduct](https://thoughtbot.com/open-source-code-of-conduct)