#ifndef __AQLIB_OBJECT_FACTORY_HPP
#define __AQLIB_OBJECT_FACTORY_HPP

#include <map>
#include <list>
#include <string>
#include <boost/shared_ptr.hpp>

namespace aqlib
{

class Serializable;

// Generic object factory
class ObjectFactory
{
protected:
    ObjectFactory() {}
public:
    virtual ~ObjectFactory() {}

    /**
     * Registers a prototype for a message of given hierarchy.
     * This is intended for all-ways compatibility:
     *  1) Client and server have the same version: 
     *     In this case the class hierarchy of the object is identical on both sides (ex: client:[Apple], server: [Apple]).
     *     The algorithm for finding a processor would be trivial and require only mapping of the class name.
     *  2) Client is newer than the server: 
     *     In this case the class name of the client object is a super-set of the server one (ex: client: [Apple,Fruit], server: [Apple])
     *     The algorithm for finding a prototype would fail if one would only look for the object class name: client sends [Apple] while
     *     server only knows about [Fruit]. To handle this case, the client sends the class hierarchy [Apple,Fruit] and the object factory 
     *     iterates it trying to find a match: if one isn't found for "Apple", it continues with "Fruit", where it succeeds.
     *  2) Client is older than the server: 
     *     In this case the class name of the client object is a sub-set of the server one (ex: client: [Fruit], server: [Apple,Fruit])
     *     The algorithm for finding a prototype would fail if the object factory would only know about the toplevel class name: client
     *     sends [Fruit] while server only maps [Apple]. To handle this case, the register algorithm iterates the entire hierarchy of the 
     *     prototype object, mapping the prototype for both "Apple" and "Fruit". The lookup algorithm will succeed then finding a 
     *     prototype for the older base class "Apple".
     */
    virtual void registerPrototype(const Serializable *prototype) = 0;

    /**
     * Returns a new object of the requested type or NULL if no class in the hierarchy is known.
     * The classHierarchy is given as a comma-separated list, with top (most specific) entries first and
     * bottom (most generic) entries last. Example: "GrannySmith,Apple,Fruit".
     * Each entry in the classHierarchy is a <classPath> defined as [<namespace>.]<class>.
     * The namespace is a dot separated list of names, from root to leaf.
     * Example: "Biology.Animals.Human, Biology.Animals.Primate, Biology.Organism".
     */
    virtual Serializable* createObject(const std::string &classHierarchy) const = 0;
};

typedef boost::shared_ptr<ObjectFactory> ObjectFactoryPtr;

} // namespace aqlib

#endif // __AQLIB_OBJECT_FACTORY_HPP
