# The name of the target program
TARGET_NAME := aquarians

# The name of the directory where the object files and application are built
BUILD_DIR := build

# The list of subdirectories containing source code files
# Current directory is included implicitly
SUBDIRS := aqlib net simulation math

# Additional include directories, to use with #include <file> syntax.
INCLUDE_DIRS := $(HOME)/Libraries/log4cxx/include

# Additional library directories
LIBRARY_DIRS := $(HOME)/Libraries/log4cxx/lib

# Additional libraries
LIBRARY_FILES := bfd log4cxx boost_regex boost_date_time

# The name of the C++ compiler
CPP := g++
 
# Compiler flags
COMPILER_INCLUDE_FLAGS := $(foreach DIR,$(INCLUDE_DIRS),-I$(DIR)) 
DEBUG_FLAGS := -g -Wall $(COMPILER_INCLUDE_FLAGS)
RELEASE_FLAGS := -O3 $(COMPILER_INCLUDE_FLAGS)

# Linker flags
LINKER_INCLUDE_FLAGS := $(foreach DIR,$(LIBRARY_DIRS),-L$(DIR)) 
LINKER_LIBRARY_FLAGS := $(foreach FILE,$(LIBRARY_FILES),-l$(FILE)) 

# Target program, complete with path
TARGET = $(BUILD_DIR)/$(TARGET_NAME)

# Debug build configuration
# Set target-specific variable value to set compiler flags
# Place this as the first target in the makefile in order to have it as default when calling 'make' without parameters
.PHONY : debug
debug : CPPFLAGS := $(DEBUG_FLAGS)
debug : $(TARGET)

# Release build configuration
# Set target-specific variable value to set compiler flags
.PHONY : release
release : CPPFLAGS := $(RELEASE_FLAGS)
release : $(TARGET)

# Build list of C++ source files fro current directory and specified subdirectories
# add current directory by default
CPP_SOURCES := $(wildcard *.cpp) $(foreach DIR,$(SUBDIRS),$(wildcard $(DIR)/*.cpp))

# Backup files, delete them with 'clean' target
# add current directory by default
BACKUP_FILES := $(wildcard *~) $(foreach DIR,$(SUBDIRS),$(wildcard $(DIR)/*~))

# Build list of C++ object and dependency files
# use 'patsubst' function to construct the object file name at the same relative path in the build folder
CPP_OBJECTS := $(patsubst %.cpp,$(BUILD_DIR)/%.o,$(CPP_SOURCES)) 
CPP_DEPENDS := $(patsubst %.cpp,$(BUILD_DIR)/%.d,$(CPP_SOURCES)) 

# Linker step, build target from object files
# Invoke the C++ compiler *without* the '-c' (compile only) flag.
# A library containing the definition of a function should appear after any object files which use it,
# therefore place the library files *after* the object files in the linker command line.
# Actually the ordering doesn't matter for shared libraries but if linking with static libraries,
# then a library containing the definition of a function should appear after any source files or object files which use it.
# For instance if hello_fn.a containing the function hello() is used from function util() defined in util_fn.a
# then the libraries must be fed to the linker in the order 'util_fn.a hello_fn.a'.
# $^ = automatic variable, the names of all the prerequisites, with spaces between them (the object files)
# -o = the name of the output file
# $@ = automatic variable, the name of the target of the rule (the target program)
# Use "-rdynamic" option to enable exception stack tracing
$(TARGET) : $(CPP_OBJECTS)
	$(CPP) -rdynamic $(LINKER_INCLUDE_FLAGS) $^ $(LINKER_LIBRARY_FLAGS) -o $@

# Include dependency info for *existing* C++ object files ( http://scottmcpeak.com/autodepend/autodepend.html )
# The '-' flag instructs make to ignore missing files, as dependency files are only
# generated for the C++ files which were successfully compiled into object files.
# See the '%.o : %.cpp' pattern rule for dependency generation.
-include $(CPP_DEPENDS)

# Pattern rule: 
#	- create destination directory for output file
#	- build object file from C++ source file
#	- generate dependency info, upon successful compilation of the source file
# -c = compile only
# $< = automatic variable, the name of the first prerequisite (the C++ source file)
# -o = the name of the output file
# $@ = automatic variable, the name of the target of the rule (the C++ object file)
# -MM = output a rule suitable for make describing the dependencies of the source file
# Use the 'patsubst' function to replace the '.cpp' suffix of the input file with '.d' for the output file.
# Use 'sed' to add the build directory prefix to the generated dependency file, that is take as input a file 
# with the first line of the form "file.o : path/file.cpp" and replace it with "path/file.o : path/file.cpp".
$(BUILD_DIR)/%.o : %.cpp
	mkdir -p $(dir $@)
	$(CPP) -c $(CPPFLAGS) $< -o $@
	$(CPP) -MM $(CPPFLAGS) $< | sed -e '1 s/\(.*\)/$(subst /,\/,$(dir $@))\1/g' > $(patsubst %.cpp,$(BUILD_DIR)/%.d,$<)

clean:
	@rm -r -f $(BUILD_DIR)
	@rm -f $(BACKUP_FILES)

run:
	@LD_LIBRARY_PATH=$$LD_LIBRARY_PATH:$(LIBRARY_DIRS) && export LD_LIBRARY_PATH && $(TARGET)
	
# Used to debug stuff
print:
	@echo $(BACKUP_FILES)

