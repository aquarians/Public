#ifndef __SERIALIZATION_SERIALIZABLE_OBJECT_HPP
#define __SERIALIZATION_SERIALIZABLE_OBJECT_HPP

#include <string>
#include <list>
#include "Archive.hpp"
#include <boost/shared_ptr.hpp>
#include <iostream>
#include <boost/regex.hpp>

namespace aqlib
{
    
// Serialization support.
// Intended to be used for communicating with a Java application, over the network.
// The same interface and encoding is implemented in the Java application too.
class Serializable
{
protected:
    Serializable() {}
public:
    virtual ~Serializable() {}
    
    // Return a comma-separated list of class names, from the top to the bottom of the hierarchy
    // Example: if GrannySmith extends Apple which extends Fruit, then return "GrannySmith,Apple,Fruit"
    // Naming convention: [<namespace>.]<class>. Example: "Apple" or "plants::fruits::Apple".
    virtual std::string getClassHierarchy() const;

    // Prototype-method: create a new object of this class, using the default constructor.
    virtual Serializable* createInstance() const = 0;

    // Read members from archive
    virtual void readFrom(ReadArchive &archive); // throws Exception
    
    // Write members to archive
    virtual void writeTo(WriteArchive &archive) const; // throws Exception
};

typedef boost::shared_ptr<Serializable> SerializablePtr;
        
// A simple collection of Serializable object prototypes
struct SerializablePrototypeRepository
{
    std::list<SerializablePtr> prototypes;

    // As calls to instance() will be made at application startup in the static initialization
    // phase, use a static function-variable instead of a static member-variable. This ensures the
    // SerializablePrototypeRepository object instance is created on-the-fly at the first call, instead
    // of worrying about initialization order and ensuring it's created before the first call to instance().
    static SerializablePrototypeRepository& instance()
    {
        static SerializablePrototypeRepository registry;
        return registry;
    }

private:
    SerializablePrototypeRepository() {}
};

// Helper class to automate adding Serializable object prototypes to the repository at application startup, 
// during the static initialization phase. To do so, declare the helper in the class header file and 
// initialize it in the implementation file. 
// Example header file:
//   class Apple: public Serializable
//   {
//     static const PrototypeRepositoryHelper PROTOTYPE_HELPER;
//   }
// Example implementation file:
//   const PrototypeRepositoryHelper Apple::PROTOTYPE_HELPER(new Apple());
// At static init, when Apple::PROTOTYPE_HELPER member variable is initialized,
// the SerializablePrototypeRepository constructor will be called with an Apple
// instance and will add it to the SerializablePrototypeRepository.
struct PrototypeRepositoryHelper
{
    PrototypeRepositoryHelper(const aqlib::SerializablePtr &prototype)
    {
        //std::cout << "PrototypeRepositoryHelper: " << prototype->getClassHierarchy() << std::endl;
        SerializablePrototypeRepository::instance().prototypes.push_back(prototype);        
    }
};

// Demonstration class for the implementation required by a Serializable object.
// There's a fair amount of boilerplate code which needs to be written for any serializable class.
// To help with it, two helper macros are defined:
//  - AQLIB_DECLARE_SERIAL
//  - AQLIB_IMPLEMENT_SERIAL
// The macros expand to EXACTLY the code in this class.
class DemoSerializableNoMacros: public Serializable
{
public:
    // A Serializable class must have a default constructor
    DemoSerializableNoMacros() {}
    
    // Helper object used to automatically add a prototype of this class to the PrototypeRepository
    // at application startup, in the static initialization phase.
    static const aqlib::PrototypeRepositoryHelper PROTOTYPE_HELPER;
    
    // Implement the Serializable interface
    std::string getClassHierarchy() const;
    
    // Implement the Serializable interface
    aqlib::Serializable* createInstance() const;
    
    // Implement the Serializable interface
    // The implementation must follow a certain pattern, see the code and the comments for it.
    void readFrom(aqlib::ReadArchive &archive);
    
    // Implement the Serializable interface
    // The implementation must follow a certain pattern, see the code and the comments for it.
    void writeTo(aqlib::WriteArchive &archive) const;
    
    // Called from readFrom(). Must read ONLY current class members.
    // Ancestor or child classes will have their own private classReadFrom() to read their members.
    void classReadFrom(aqlib::ReadArchive &archive) {}
    
    // Called from writeTo(). Must write ONLY current class members.
    // Ancestor or child classes will have their own private classWriteTo() to write their members.    
    void classWriteTo(aqlib::WriteArchive &archive) const {}
};

// Implementation of AQLIB_DECLARE_SERIAL macro. 
// See the code of DemoSerializableNoMacros class header for an explanation.
#define AQLIB_DECLARE_SERIAL \
    static const aqlib::PrototypeRepositoryHelper PROTOTYPE_HELPER; \
    std::string getClassHierarchy() const; \
    aqlib::Serializable* createInstance() const; \
    void readFrom(aqlib::ReadArchive &archive); \
    void writeTo(aqlib::WriteArchive &archive) const; \
    void classReadFrom(aqlib::ReadArchive &archive); \
    void classWriteTo(aqlib::WriteArchive &archive) const


// Implementation of AQLIB_IMPLEMENT_SERIAL macro.
// See the code of DemoSerializableNoMacros class implementation for an explanation.
// Preferably use the full class name, ex: "plants::fruits::Apple" instead or "Apple",
// or as the application grows, you'll run into name conflicts at some point.
#define AQLIB_IMPLEMENT_SERIAL(class_name, base_class_name) \
    const aqlib::PrototypeRepositoryHelper class_name::PROTOTYPE_HELPER(aqlib::SerializablePtr(new class_name())); \
    std::string class_name::getClassHierarchy() const \
    { \
        std::string baseHierarchy = base_class_name::getClassHierarchy(); \
        std::string className = boost::regex_replace(std::string(#class_name), boost::regex("::"), "."); \
        return baseHierarchy.size() ? className + "," + baseHierarchy : className; \
    } \
    aqlib::Serializable* class_name::createInstance() const \
    { \
        return new class_name(); \
    } \
    void class_name::writeTo(aqlib::WriteArchive &archive) const \
    { \
        base_class_name::writeTo(archive); \
        boost::shared_ptr<aqlib::WriteArchive> classArchive(archive.createInstance()); \
        classWriteTo(*(classArchive.get())); \
        std::string classData = classArchive->getData(); \
        archive.writeString(#class_name, classData); \
    } \
    void class_name::readFrom(aqlib::ReadArchive &archive) \
    { \
        base_class_name::readFrom(archive); \
        if (!archive.hasMoreData(#class_name)) return; \
        std::string classData = archive.readString(#class_name); \
        boost::shared_ptr<aqlib::ReadArchive> classArchive(archive.createInstance(classData.data(), classData.size())); \
        classReadFrom(*(classArchive.get())); \
    }

// Example usage of the serialization macros.
// Has exactly the same code as the manually-coded DemoSerializableNoMacros but with far less effort.
class DemoSerializableWithMacros: public Serializable
{
    // Some example members
    int mIntValue;
    double mDoubleValue;
    std::string mStringValue;
    boost::posix_time::ptime mTimeValue;
    boost::shared_ptr<DemoSerializableNoMacros> mObjectValue;
    
public:
    DemoSerializableWithMacros() {}

    // Declare the Serializable interface
    AQLIB_DECLARE_SERIAL;
};

} // namespace aqlib

#endif // __SERIALIZATION_SERIALIZABLE_OBJECT_HPP
