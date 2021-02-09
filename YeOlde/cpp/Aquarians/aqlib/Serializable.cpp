#include "Serializable.hpp"
#include "BinaryArchive.hpp"

namespace aqlib
{
    
std::string Serializable::getClassHierarchy() const
{
    // Return empty. Implementation provided solely for easy implementation of AQLIB_IMPLEMENT_SERIAL macro,
    // that is given a base class (including Serializable), be able to call base_class::getClassHierarchy().    
    return "";
}

void Serializable::readFrom(ReadArchive &archive)
{
    // Do nothing. Implementation provided solely for easy implementation of AQLIB_IMPLEMENT_SERIAL macro,
    // that is given a base class (including Serializable), be able to call base_class::readFrom().
}

void Serializable::writeTo(WriteArchive &archive) const
{
    // Do nothing. Implementation provided solely for easy implementation of AQLIB_IMPLEMENT_SERIAL macro,
    // that is given a base class (including Serializable), be able to call base_class::writeTo().    
}

const aqlib::PrototypeRepositoryHelper aqlib::DemoSerializableNoMacros::PROTOTYPE_HELPER(aqlib::SerializablePtr(new aqlib::DemoSerializableNoMacros()));

std::string aqlib::DemoSerializableNoMacros::getClassHierarchy() const
{
    // Return a comma separated list of class names, from the highest to the lowest in the hierarcy.
    // Example: "GrannySmith,Apple,Fruit".
    // Use regex_replace to replace the C++ namespace separator "::" with a dot "." (Java style).
    // For instance "varieties::GrannySmith,plants::fruits::Apple" becomes "varieties.GrannySmith,plants.fruits.Apple".
    std::string baseHierarchy = aqlib::Serializable::getClassHierarchy();
    std::string className = boost::regex_replace(std::string("aqlib::DemoSerializableNoMacros"), boost::regex("::"), ".");
    return baseHierarchy.size() ? className + "," + baseHierarchy : className;
} 

aqlib::Serializable* aqlib::DemoSerializableNoMacros::createInstance() const 
{
    return new aqlib::DemoSerializableNoMacros();
}

void aqlib::DemoSerializableNoMacros::writeTo(aqlib::WriteArchive &archive) const 
{ 
    // Make sure you ALWAYS call the base class writeTo() first.
    // This is needed to ensure up-down compatibility.
    // In a binary archive, which is a stream of data and where order matters, the base class data will be written first.
    // When reading, one must also ensure that base class readFrom() is called first.
    // In the example hierarchy: GrannySmith which extends Apple which extends Fruit,
    // the archive will contain data in the order (from left to right): <Fruit>[<Apple>][<GrannySmith>],
    // meaning a Fruit data will always exist, but Apple or GrannySmith may not, if they were added in later
    // application versions. When reading the stream, the Fruit data will always be read first, then,
    // depending on weather the reader class is an Apple or GrannySmith it'll read the subsequent data,
    // else will simply ignore it.    
    Serializable::writeTo(archive);
    
    // Pack current class data into it's own archive, serialized distinctly.
    // This is needed for up-down compatibility.
    // When reading, one de-serializes the whole class data, but the current object may not read all of it and might not have all of it.
    // Consider the case of an Apple class who has a taste member, denoted Apple{taste}. 
    // At a later application version, a <color> member is added, the class being denoted as Apple{taste, color}.
    // The cases:
    // 1) Archive contains <taste>, reader is an Apple{taste}.
    //      Trivial case, the reader reads <taste> and gets to the end of the class archive.
    // 2) Archive contains <taste><color> (in this order), reader is an Apple{taste, color}. 
    //      Trivial case again, the reader reads <taste>, then <color> and gets to the end of the class archive.
    // 3) Archive contains <taste>, reader is an Apple{taste, color}
    //      The reader may read <taste> without any check but it'll generate an exception if it attempts 
    //      to read the unexising <color>. Therefore the need for Archive.hasMoreData() method - if it
    //      returns false, the reader knows he's loading an older version and skips reading <taste>.
    // 4) Archive contains <taste><color>, reader is an Apple{taste}
    //      The reader will read <taste> without any check but knows nothing about <color>.
    //      If the reading pointer isn't advanced over the <color> member, the next reads will be undefined.
    //      To solve this problem, the entire data of the class is packed in it's own archive.
    //      So even if the Apple{taste} class doesn't read all the data in the <taste><color> stream, the stream
    //      itself is part of a larger one [<Prev data>]<Apple data>[<Next data>] and the entire <Apple data>
    //      chunk containing <taste><color> is read at once.
    boost::shared_ptr<aqlib::WriteArchive> classArchive(archive.createInstance());
    classWriteTo(*(classArchive.get()));
    std::string classData = classArchive->getData();
    archive.writeString("DemoSerializableNoMacros", classData);
}

void aqlib::DemoSerializableNoMacros::readFrom(aqlib::ReadArchive &archive)
{ 
    // Make sure you ALWAYS call the base class readFrom() first.
    // This is needed to ensure up-down compatibility.
    // See the comments of writeTo() for an explanation of the reading code.
    Serializable::readFrom(archive);
    
    // Stored object might not have this class data, for instance in the hierarchy "Apple,Fruit",
    // this might be an Apple but the stored object could be an older version plain Fruit.
    if (!archive.hasMoreData("DemoSerializableNoMacros")) return;

    // Class data exists, read all of it then pass to classReadFrom() for extracting each member
    std::string classData = archive.readString("DemoSerializableNoMacros");
    boost::shared_ptr<aqlib::ReadArchive> classArchive(archive.createInstance(classData.data(), classData.size()));
    classReadFrom(*(classArchive.get()));
} 

// Implement the Serializable interface except for classReadFrom)() and classWriteTo().
// Notice using the full class name, "aqlib::DemoSerializableWithMacros" instead of just "DemoSerializableWithMacros".
AQLIB_IMPLEMENT_SERIAL(aqlib::DemoSerializableWithMacros, aqlib::Serializable);

void DemoSerializableWithMacros::classReadFrom(aqlib::ReadArchive &archive) 
{
    // Say IntValue, DoubleValue, StringValue were there from the first version
    mIntValue = archive.readInt("IntValue");
    mDoubleValue = archive.readFloat("DoubleValue");
    mStringValue = archive.readString("StringValue");
    // TimeValue and ObjectValue might have been added on a later version
    if (!archive.hasMoreData("TimeValue")) return;
    mTimeValue = archive.readTime("TimeValue");
    mObjectValue = boost::shared_ptr<DemoSerializableNoMacros>(dynamic_cast<DemoSerializableNoMacros *>(archive.readObject("ObjectValue")));
}

void DemoSerializableWithMacros::classWriteTo(aqlib::WriteArchive &archive) const
{
    archive.writeInt("IntValue", mIntValue);
    archive.writeFloat("DoubleValue", mDoubleValue);
    archive.writeString("StringValue", mStringValue);
    archive.writeTime("TimeValue", mTimeValue);
    archive.writeObject("ObjectValue", mObjectValue.get());
}

} // namespace aqlib
