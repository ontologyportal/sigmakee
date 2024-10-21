from neo4j import GraphDatabase
from py4j.java_gateway import JavaGateway
import os

# small python routine as part of a process to convert SUMO's simple tuple content into a Neo4j graph.
# Note that most of SUMO's logical content is necessarily lost in such an export.
# You need to run a recent JDK for Neo4j to work.  I have a script to change JDK
# to_openjdk17
# neo4j start
# This code depends upon having SigmaKEE installed and having run the following to create the tuples (you must change to conform to your paths)
# $ java -Xmx14g -cp /home/theuser/workspace/sigmakee/lib/*:/home/theuser/workspace/sigmakee/build/WEB-INF/classes com.articulate.sigma.KButilities -r
# Note that the java code will write to a file called triples.txt that the python code will read from
# You will need to run
#   pip3 install neo4j
#   pip3 install py4j
# License: LGPL
# author: Adam Pease

class SUMOasNeo:

    def __init__(self, uri, user, password):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))
        self.documentation = dict()

    def close(self):
        self.driver.close()

    def run(self,str):
        print("run: ",str)
        with self.driver.session() as session:
            greeting = session.execute_write(self.process,str)
            print(greeting)

    def is_number(self,n):
        is_number = True
        try:
            num = float(n)
            # check for "nan" floats
            is_number = num == num   # or use `math.isnan(num)`
        except ValueError:
            is_number = False
        return is_number

    @staticmethod
    def process(tx,l):
        #result = tx.run("MATCH (n) DETACH DELETE n")
        #result = tx.run("CREATE (Physical:Class {label: \"Physical\"}) RETURN Physical.label")
        #print("result: ",result.single())
        #result = tx.run("CREATE (Entity:Class {label: \"Entity\"}) RETURN Entity.label")
        #print("result: ",result.single())
        #result = tx.run("CREATE (Entity)<-[r:subclass]-(Physical) ")
        #print("result: ",result.single())
        #result = tx.run("MATCH (a:Class) RETURN a ")
        #values = []
        #for record in result:
        #    values.append(record.values())
        #summary = result.consume()
        #for val in values:
        #    print(val)
        result = tx.run(l)
        print(result)

    def readDoc(self):
        result = []
        file1 = open(os.getenv('SIGMA_HOME') + '/KBs/triples.txt', 'r')
        Lines = file1.readlines()
        for line in Lines:
            triple = line.strip().split("|")
            if "-" in triple[0]:
                triple[0] = triple[0].replace("-","_")
            if len(triple) == 3:
                if triple[1] == "documentation":
                    self.documentation[triple[0]] = triple[2]
                #print(triple)
        file1.close()

    def readTriples(self):
        result = []
        file1 = open(os.getenv('SIGMA_HOME') + '/KBs/triples.txt', 'r')
        Lines = file1.readlines()
        for line in Lines:
            triple = line.strip().split("|")
            if len(triple) == 2:
                if not self.is_number(triple[0]):
                    if "-" in triple[0]:
                        triple[0] = triple[0].replace("-","_")
                    str = "CREATE (" + triple[0] + ":Thing {label: \"" + triple[0] + "\", url:\"https://sigma.ontologyportal.org:8443/sigma/Browse.jsp?lang=EnglishLanguage&flang=SUO-KIF&kb=SUMO&term=" + triple[0] + "\""
                    if self.documentation.get(triple[0]):
                        str += ", doc:" + self.documentation[triple[0]] + ""
                    str += "})"
                    result.append(str)
                #print(triple[0])
            else:
                #if "\"" not in triple[2]:
                if triple[1] != "documentation":
                    if "-" in triple[0]:
                        triple[0] = triple[0].replace("-","_")
                    if "-" in triple[2]:
                        triple[2] = triple[2].replace("-","_")
                    result.append("MATCH (a:Thing), (b:Thing) WHERE a.label = \"" + triple[0] + "\" AND b.label = \"" + triple[2] + "\" " + "CREATE (a)-[:" + triple[1] + "]->(b)")
                #print(triple)
        file1.close()
        return result

if __name__ == "__main__":
    greeter = SUMOasNeo("bolt://localhost:7687", "neo4j", "password")
    greeter.readDoc()
    list = greeter.readTriples()
    for l in list:
        greeter.run(l)
    greeter.close()


