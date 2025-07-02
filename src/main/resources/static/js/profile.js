document.addEventListener('DOMContentLoaded', function() {
    // Initialize authentication
    const token = auth.getToken();
    if (!token) {
        window.location.href = '/login';
        return;
    }
    
    // Load user profile
    loadUserProfile();
    
    // Function to load user profile data
    function loadUserProfile() {
        const profileContent = document.getElementById('profile-content');
        
        fetch('/api/profile', {
            headers: {
                'Authorization': 'Bearer ' + token
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to load profile');
            }
            return response.json();
        })
        .then(data => {
            console.log('Profile data:', data);
            
            // Display the appropriate profile template based on user type
            if (data.userType === 'candidate') {
                displayCandidateProfile(data.user);
            } else if (data.userType === 'recruiter') {
                displayRecruiterProfile(data.user, data.company);
            } else {
                throw new Error('Unknown user type');
            }
        })
        .catch(error => {
            console.error('Error loading profile:', error);
            profileContent.innerHTML = `
                <div class="alert alert-danger">
                    <i class="bi bi-exclamation-triangle"></i> ${error.message || 'Failed to load profile data'}
                </div>
                <button class="btn btn-primary" onclick="window.location.href='/dashboard'">
                    <i class="bi bi-arrow-left"></i> Return to Dashboard
                </button>
            `;
        });
    }
    
    // Function to display candidate profile
    function displayCandidateProfile(user) {
        const profileContent = document.getElementById('profile-content');
        const template = document.getElementById('candidate-profile-template');
        
        // Clone the template
        const profileElement = template.content.cloneNode(true);
        profileContent.innerHTML = '';
        profileContent.appendChild(profileElement);
        
        // Set profile header information
        document.getElementById('profile-name').textContent = `${user.firstName} ${user.lastName}`;
        document.getElementById('profile-email').textContent = user.email;
        
        // Set profile picture
        const profilePictureContainer = document.getElementById('profile-picture-container');
        if (user.profilePicturePath) {
            // Add a timestamp to the image URL to prevent caching
            const imageUrl = `/uploads/${user.profilePicturePath}?t=${new Date().getTime()}`;
            console.log('Profile picture path:', user.profilePicturePath);
            console.log('Full image URL:', imageUrl);
            profilePictureContainer.innerHTML = `
                <img src="${imageUrl}" class="profile-picture" alt="Profile picture" onerror="console.error('Failed to load image:', this.src)">
            `;
        } else {
            profilePictureContainer.innerHTML = `
                <div class="profile-picture-placeholder">
                    <i class="bi bi-person-circle"></i>
                </div>
            `;
        }
        
        // Fill form fields
        document.getElementById('firstName').value = user.firstName || '';
        document.getElementById('lastName').value = user.lastName || '';
        document.getElementById('email').value = user.email || '';
        document.getElementById('phone').value = user.phone || '';
        document.getElementById('address').value = user.address || '';
        document.getElementById('bio').value = user.bio || '';
        
        // Show CV if available
        if (user.cvPath) {
            document.getElementById('cv-preview').innerHTML = `
                <p><strong>CV actuel:</strong> <a href="/uploads/${user.cvPath}" target="_blank">${getFilenameFromPath(user.cvPath)}</a></p>
            `;
            document.getElementById('cv-preview').style.display = 'block';
        }
        
        // Get collections from the candidate entity
        const skills = user.skills || [];
        const education = user.education || [];
        const experience = user.experience || [];
        
        console.log('Candidate data loaded:', {
            skills: skills,
            education: education,
            experience: experience
        });
        
        // Setup skills, education, and experience sections
        setupSkillsSection(skills);
        setupEducationSection(education);
        setupExperienceSection(experience);
        
        // Set up event listeners for form submissions
        setupCandidateFormSubmission();
        
        // Set up event listeners for file uploads
        setupFileUploads();
        
        // Set up event listeners for add buttons
        setupAddButtons();
    }
    
    // Setup skills section with existing data
    function setupSkillsSection(skills) {
        // Find the skills section
        const sections = document.querySelectorAll('.profile-section');
        let skillsSection = null;
        
        // Find section by title text
        sections.forEach(section => {
            const title = section.querySelector('h2.section-title');
            if (title && title.textContent.includes('Compétences')) {
                skillsSection = section;
            }
        });
        
        if (!skillsSection) return;
        
        // Create container if it doesn't exist
        let skillsContainer = skillsSection.querySelector('.skills-container');
        if (!skillsContainer) {
            skillsContainer = document.createElement('div');
            skillsContainer.className = 'skills-container';
            
            // Find a good place to insert the container (after the input group)
            const inputGroup = skillsSection.querySelector('.input-group');
            if (inputGroup) {
                inputGroup.parentNode.insertBefore(skillsContainer, inputGroup.nextSibling);
            } else {
                skillsSection.appendChild(skillsContainer);
            }
        }
        
        // Try to find the skills input element
        const skillInputField = skillsSection.querySelector('input[type="text"]');
        if (skillInputField) {
            skillInputField.value = '';  // Clear input field to avoid confusion
            
            // Set an input event listener to check for duplicates
            skillInputField.addEventListener('input', function() {
                const value = this.value.trim();
                const existingSkills = Array.from(document.querySelectorAll('.skill-item span')).map(span => span.textContent);
                
                if (existingSkills.includes(value)) {
                    this.classList.add('is-invalid');
                    // Show warning if not already present
                    let warning = skillsSection.querySelector('.duplicate-warning');
                    if (!warning) {
                        warning = document.createElement('div');
                        warning.className = 'duplicate-warning text-danger small mt-1';
                        warning.textContent = 'Cette compétence existe déjà';
                        this.parentNode.appendChild(warning);
                    }
                } else {
                    this.classList.remove('is-invalid');
                    // Remove warning if present
                    const warning = skillsSection.querySelector('.duplicate-warning');
                    if (warning) {
                        warning.remove();
                    }
                }
            });
        }
        
        // Display existing skills with delete buttons
        // Clear previous skills display
        const existingSkillsList = skillsContainer.querySelector('.existing-skills-list');
        if (existingSkillsList) {
            existingSkillsList.remove();
        }
        
        // Create a list to display existing skills
        if (skills.length > 0) {
            const skillsList = document.createElement('div');
            skillsList.className = 'existing-skills-list mt-3';
            
            // Create a Set to store unique skills
            const uniqueSkills = new Set(skills);
            
            uniqueSkills.forEach(skill => {
                const skillItem = document.createElement('div');
                skillItem.className = 'skill-item d-flex justify-content-between align-items-center mb-2 p-2 border rounded';
                
                const skillText = document.createElement('span');
                skillText.textContent = skill;
                
                const deleteBtn = document.createElement('button');
                deleteBtn.className = 'btn btn-sm btn-danger';
                deleteBtn.innerHTML = '<i class="bi bi-trash"></i> Supprimer';
                deleteBtn.addEventListener('click', function() {
                    deleteSkill(skill);
                });
                
                skillItem.appendChild(skillText);
                skillItem.appendChild(deleteBtn);
                skillsList.appendChild(skillItem);
            });
            
            skillsContainer.appendChild(skillsList);
        }
        
        // Setup "Ajouter" button click handlers
        const addButton = skillsSection.querySelector('button.btn.btn-primary');
        if (addButton) {
            // Remove previous event listeners by cloning and replacing the button
            const newButton = addButton.cloneNode(true);
            addButton.parentNode.replaceChild(newButton, addButton);
            
            newButton.addEventListener('click', function(event) {
                const skillInput = skillsSection.querySelector('input[type="text"]');
                if (skillInput && skillInput.value.trim()) {
                    // Check for duplicates
                    const existingSkills = Array.from(document.querySelectorAll('.skill-item span')).map(span => span.textContent);
                    if (existingSkills.includes(skillInput.value.trim())) {
                        // Already exists, show warning
                        skillInput.classList.add('is-invalid');
                        // Show warning if not already present
                        let warning = skillsSection.querySelector('.duplicate-warning');
                        if (!warning) {
                            warning = document.createElement('div');
                            warning.className = 'duplicate-warning text-danger small mt-1';
                            warning.textContent = 'Cette compétence existe déjà';
                            skillInput.parentNode.appendChild(warning);
                        }
                        return;
                    }
                    
                    // Here we would normally add a new skill to the UI
                    // For now, just save the form with the current value
                    document.getElementById('save-profile-btn').click();
                }
            });
        }
    }
    
    // Setup education section with existing data
    function setupEducationSection(education) {
        // Find the education section
        const sections = document.querySelectorAll('.profile-section');
        let educationSection = null;
        
        // Find section by title text
        sections.forEach(section => {
            const title = section.querySelector('h2.section-title');
            if (title && title.textContent.includes('Formation')) {
                educationSection = section;
            }
        });
        
        if (!educationSection) return;
        
        // Create container if it doesn't exist
        let educationContainer = educationSection.querySelector('.education-container');
        if (!educationContainer) {
            educationContainer = document.createElement('div');
            educationContainer.className = 'education-container';
            
            // Find a good place to insert the container (after the input group)
            const inputGroup = educationSection.querySelector('.input-group');
            if (inputGroup) {
                inputGroup.parentNode.insertBefore(educationContainer, inputGroup.nextSibling);
            } else {
                educationSection.appendChild(educationContainer);
            }
        }
        
        // Try to find the education input element
        const educationInputField = educationSection.querySelector('input[type="text"]');
        if (educationInputField) {
            educationInputField.value = '';  // Clear input field to avoid confusion
            
            // Set an input event listener to check for duplicates
            educationInputField.addEventListener('input', function() {
                const value = this.value.trim();
                const existingEducations = Array.from(document.querySelectorAll('.education-item span')).map(span => span.textContent);
                
                if (existingEducations.includes(value)) {
                    this.classList.add('is-invalid');
                    // Show warning if not already present
                    let warning = educationSection.querySelector('.duplicate-warning');
                    if (!warning) {
                        warning = document.createElement('div');
                        warning.className = 'duplicate-warning text-danger small mt-1';
                        warning.textContent = 'Cette formation existe déjà';
                        this.parentNode.appendChild(warning);
                    }
                } else {
                    this.classList.remove('is-invalid');
                    // Remove warning if present
                    const warning = educationSection.querySelector('.duplicate-warning');
                    if (warning) {
                        warning.remove();
                    }
                }
            });
        }
        
        // Display existing education entries with delete buttons
        // Clear previous education display
        const existingEducationList = educationContainer.querySelector('.existing-education-list');
        if (existingEducationList) {
            existingEducationList.remove();
        }
        
        // Create a list to display existing education entries
        if (education.length > 0) {
            const educationList = document.createElement('div');
            educationList.className = 'existing-education-list mt-3';
            
            // Create a Set to store unique education entries
            const uniqueEducation = new Set(education);
            
            uniqueEducation.forEach(edu => {
                const educationItem = document.createElement('div');
                educationItem.className = 'education-item d-flex justify-content-between align-items-center mb-2 p-2 border rounded';
                
                const educationText = document.createElement('span');
                educationText.textContent = edu;
                
                const deleteBtn = document.createElement('button');
                deleteBtn.className = 'btn btn-sm btn-danger';
                deleteBtn.innerHTML = '<i class="bi bi-trash"></i> Supprimer';
                deleteBtn.addEventListener('click', function() {
                    deleteEducation(edu);
                });
                
                educationItem.appendChild(educationText);
                educationItem.appendChild(deleteBtn);
                educationList.appendChild(educationItem);
            });
            
            educationContainer.appendChild(educationList);
        }
        
        // Setup "Ajouter" button click handlers
        const addButton = educationSection.querySelector('button.btn.btn-primary');
        if (addButton) {
            // Remove previous event listeners by cloning and replacing the button
            const newButton = addButton.cloneNode(true);
            addButton.parentNode.replaceChild(newButton, addButton);
            
            newButton.addEventListener('click', function() {
                const educationInput = educationSection.querySelector('input[type="text"]');
                if (educationInput && educationInput.value.trim()) {
                    // Check for duplicates
                    const existingEducations = Array.from(document.querySelectorAll('.education-item span')).map(span => span.textContent);
                    if (existingEducations.includes(educationInput.value.trim())) {
                        // Already exists, show warning
                        educationInput.classList.add('is-invalid');
                        // Show warning if not already present
                        let warning = educationSection.querySelector('.duplicate-warning');
                        if (!warning) {
                            warning = document.createElement('div');
                            warning.className = 'duplicate-warning text-danger small mt-1';
                            warning.textContent = 'Cette formation existe déjà';
                            educationInput.parentNode.appendChild(warning);
                        }
                        return;
                    }
                    
                    document.getElementById('save-profile-btn').click();
                }
            });
        }
    }
    
    // Setup experience section with existing data
    function setupExperienceSection(experience) {
        // Find the experience section
        const sections = document.querySelectorAll('.profile-section');
        let experienceSection = null;
        
        // Find section by title text
        sections.forEach(section => {
            const title = section.querySelector('h2.section-title');
            if (title && title.textContent.includes('Expérience')) {
                experienceSection = section;
            }
        });
        
        if (!experienceSection) return;
        
        // Create container if it doesn't exist
        let experienceContainer = experienceSection.querySelector('.experience-container');
        if (!experienceContainer) {
            experienceContainer = document.createElement('div');
            experienceContainer.className = 'experience-container';
            
            // Find a good place to insert the container (after the input group)
            const inputGroup = experienceSection.querySelector('.input-group');
            if (inputGroup) {
                inputGroup.parentNode.insertBefore(experienceContainer, inputGroup.nextSibling);
            } else {
                experienceSection.appendChild(experienceContainer);
            }
        }
        
        // Try to find the experience input element
        const experienceInputField = experienceSection.querySelector('input[type="text"]');
        if (experienceInputField) {
            experienceInputField.value = '';  // Clear input field to avoid confusion
            
            // Set an input event listener to check for duplicates
            experienceInputField.addEventListener('input', function() {
                const value = this.value.trim();
                const existingExperiences = Array.from(document.querySelectorAll('.experience-item span')).map(span => span.textContent);
                
                if (existingExperiences.includes(value)) {
                    this.classList.add('is-invalid');
                    // Show warning if not already present
                    let warning = experienceSection.querySelector('.duplicate-warning');
                    if (!warning) {
                        warning = document.createElement('div');
                        warning.className = 'duplicate-warning text-danger small mt-1';
                        warning.textContent = 'Cette expérience existe déjà';
                        this.parentNode.appendChild(warning);
                    }
                } else {
                    this.classList.remove('is-invalid');
                    // Remove warning if present
                    const warning = experienceSection.querySelector('.duplicate-warning');
                    if (warning) {
                        warning.remove();
                    }
                }
            });
        }
        
        // Display existing experience entries with delete buttons
        // Clear previous experience display
        const existingExperienceList = experienceContainer.querySelector('.existing-experience-list');
        if (existingExperienceList) {
            existingExperienceList.remove();
        }
        
        // Create a list to display existing experience entries
        if (experience.length > 0) {
            const experienceList = document.createElement('div');
            experienceList.className = 'existing-experience-list mt-3';
            
            // Create a Set to store unique experience entries
            const uniqueExperience = new Set(experience);
            
            uniqueExperience.forEach(exp => {
                const experienceItem = document.createElement('div');
                experienceItem.className = 'experience-item d-flex justify-content-between align-items-center mb-2 p-2 border rounded';
                
                const experienceText = document.createElement('span');
                experienceText.textContent = exp;
                
                const deleteBtn = document.createElement('button');
                deleteBtn.className = 'btn btn-sm btn-danger';
                deleteBtn.innerHTML = '<i class="bi bi-trash"></i> Supprimer';
                deleteBtn.addEventListener('click', function() {
                    deleteExperience(exp);
                });
                
                experienceItem.appendChild(experienceText);
                experienceItem.appendChild(deleteBtn);
                experienceList.appendChild(experienceItem);
            });
            
            experienceContainer.appendChild(experienceList);
        }
        
        // Setup "Ajouter" button click handlers - match skills button handler
        const addButton = experienceSection.querySelector('button.btn.btn-primary');
        if (addButton) {
            // Remove previous event listeners by cloning and replacing the button
            const newButton = addButton.cloneNode(true);
            addButton.parentNode.replaceChild(newButton, addButton);
            
            newButton.addEventListener('click', function(event) {
                event.preventDefault(); // Prevent form submission
                
                const experienceInput = experienceSection.querySelector('input[type="text"]');
                if (experienceInput && experienceInput.value.trim()) {
                    // Check for duplicates
                    const existingExperiences = Array.from(document.querySelectorAll('.experience-item span')).map(span => span.textContent);
                    
                    if (existingExperiences.includes(experienceInput.value.trim())) {
                        // Already exists, show warning
                        experienceInput.classList.add('is-invalid');
                        // Show warning if not already present
                        let warning = experienceSection.querySelector('.duplicate-warning');
                        if (!warning) {
                            warning = document.createElement('div');
                            warning.className = 'duplicate-warning text-danger small mt-1';
                            warning.textContent = 'Cette expérience existe déjà';
                            experienceInput.parentNode.appendChild(warning);
                        }
                        return;
                    }
                    
                    // Use the same approach as skills - don't use the direct method
                    collectAndSubmitProfileData(experienceInput.value.trim(), 'experience');
                }
            });
        }
    }
    
    // Function to delete a skill
    function deleteSkill(skill) {
        if (!skill || !confirm(`Êtes-vous sûr de vouloir supprimer la compétence "${skill}"?`)) {
            return;
        }
        
        // Get all existing skills except the one to delete
        const skills = [];
        const skillItems = document.querySelectorAll('.skill-item span');
        skillItems.forEach(item => {
            if (item.textContent !== skill) {
                skills.push(item.textContent);
            }
        });
        
        // Get all existing education and experience
        const education = [];
        const educationItems = document.querySelectorAll('.education-item span');
        educationItems.forEach(item => {
            education.push(item.textContent);
        });
        
        const experience = [];
        const experienceItems = document.querySelectorAll('.experience-item span');
        experienceItems.forEach(item => {
            experience.push(item.textContent);
        });
        
        // Get current form data
        const formData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            phone: document.getElementById('phone').value,
            address: document.getElementById('address').value,
            bio: document.getElementById('bio').value,
            skills: skills,
            education: education,
            experience: experience
        };
        
        console.log('Submitting updated profile data after skill deletion:', formData);
        
        // Submit the data
        submitProfileData(formData, 'Skill deleted successfully');
    }
    
    // Function to delete an education entry
    function deleteEducation(education) {
        if (!education || !confirm(`Êtes-vous sûr de vouloir supprimer la formation "${education}"?`)) {
            return;
        }
        
        // Get all existing skills
        const skills = [];
        const skillItems = document.querySelectorAll('.skill-item span');
        skillItems.forEach(item => {
            skills.push(item.textContent);
        });
        
        // Get all existing education except the one to delete
        const educationArray = [];
        const educationItems = document.querySelectorAll('.education-item span');
        educationItems.forEach(item => {
            if (item.textContent !== education) {
                educationArray.push(item.textContent);
            }
        });
        
        // Get all existing experience
        const experience = [];
        const experienceItems = document.querySelectorAll('.experience-item span');
        experienceItems.forEach(item => {
            experience.push(item.textContent);
        });
        
        // Get current form data
        const formData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            phone: document.getElementById('phone').value,
            address: document.getElementById('address').value,
            bio: document.getElementById('bio').value,
            skills: skills,
            education: educationArray,
            experience: experience
        };
        
        console.log('Submitting updated profile data after education deletion:', formData);
        
        // Submit the data
        submitProfileData(formData, 'Education deleted successfully');
    }
    
    // Function to delete an experience entry
    function deleteExperience(experience) {
        if (!experience || !confirm(`Êtes-vous sûr de vouloir supprimer l'expérience "${experience}"?`)) {
            return;
        }
        
        // Get all existing skills
        const skills = [];
        const skillItems = document.querySelectorAll('.skill-item span');
        skillItems.forEach(item => {
            skills.push(item.textContent);
        });
        
        // Get all existing education
        const education = [];
        const educationItems = document.querySelectorAll('.education-item span');
        educationItems.forEach(item => {
            education.push(item.textContent);
        });
        
        // Get all existing experience except the one to delete
        const experienceArray = [];
        const experienceItems = document.querySelectorAll('.experience-item span');
        experienceItems.forEach(item => {
            if (item.textContent !== experience) {
                experienceArray.push(item.textContent);
            }
        });
        
        // Get current form data
        const formData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            phone: document.getElementById('phone').value,
            address: document.getElementById('address').value,
            bio: document.getElementById('bio').value,
            skills: skills,
            education: education,
            experience: experienceArray
        };
        
        console.log('Submitting updated profile data after experience deletion:', formData);
        
        // Submit the data
        submitProfileData(formData, 'Experience deleted successfully');
    }
    
    // Helper function to submit profile data
    function submitProfileData(formData, successMessage) {
        const saveButton = document.getElementById('save-profile-btn');
        const errorMessage = document.getElementById('profile-error');
        const successMessageElement = document.getElementById('profile-success');
        
        // Disable button and show loading state
        if (saveButton) {
            saveButton.disabled = true;
            saveButton.innerHTML = '<div class="loading-spinner"></div> Enregistrement...';
        }
        if (errorMessage) errorMessage.style.display = 'none';
        if (successMessageElement) successMessageElement.style.display = 'none';
        
        // Send the data
        fetch('/api/profile/candidate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + auth.getToken()
            },
            body: JSON.stringify(formData)
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(err => {
                    throw new Error(err.message || 'Failed to update profile');
                });
            }
            return response.json();
        })
        .then(data => {
            console.log('Update response:', data);
            
            // Show success message
            if (successMessageElement) {
                successMessageElement.textContent = successMessage || data.message || 'Profile updated successfully';
                successMessageElement.style.display = 'block';
            }
            
            // Reset button state
            if (saveButton) {
                saveButton.disabled = false;
                saveButton.innerHTML = '<i class="bi bi-save"></i> Enregistrer';
            }
            
            // Reload profile after 1 second to see updated data
            setTimeout(loadUserProfile, 1000);
        })
        .catch(error => {
            console.error('Error updating profile:', error);
            
            // Show error message
            if (errorMessage) {
                errorMessage.textContent = error.message || 'Failed to update profile';
                errorMessage.style.display = 'block';
            }
            
            // Reset button state
            if (saveButton) {
                saveButton.disabled = false;
                saveButton.innerHTML = '<i class="bi bi-save"></i> Enregistrer';
            }
        });
    }
    
    // Setup add buttons for skills, education, and experience
    function setupAddButtons() {
        // Find all sections by title text
        const sections = document.querySelectorAll('.profile-section');
        let skillsSection, educationSection, experienceSection;
        
        // Find sections by their titles
        sections.forEach(section => {
            const title = section.querySelector('h2.section-title');
            if (title) {
                if (title.textContent.includes('Compétences')) {
                    skillsSection = section;
                } else if (title.textContent.includes('Formation')) {
                    educationSection = section;
                } else if (title.textContent.includes('Expérience')) {
                    experienceSection = section;
                }
            }
        });
        
        // Remove old event listeners and add new ones
        setupSectionButtonHandlers(skillsSection, 'skills');
        setupSectionButtonHandlers(educationSection, 'education');
        setupSectionButtonHandlers(experienceSection, 'experience');
    }
    
    // Helper function to set up button handlers for a section
    function setupSectionButtonHandlers(section, type) {
        if (!section) return;
        
        const addButton = section.querySelector('button.btn.btn-primary');
        if (addButton) {
            // Remove old event listeners by cloning
            const newButton = addButton.cloneNode(true);
            addButton.parentNode.replaceChild(newButton, addButton);
            
            newButton.addEventListener('click', function(event) {
                event.preventDefault(); // Prevent default form submission
                
                const inputField = section.querySelector('input[type="text"]');
                if (inputField && inputField.value.trim()) {
                    // Collect all current values from form inputs
                    collectAndSubmitProfileData(inputField.value.trim(), type);
                }
            });
        }
    }
    
    // Function to collect form data and submit it
    function collectAndSubmitProfileData(newValue, type) {
        // Get current form data
        const formData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            phone: document.getElementById('phone').value,
            address: document.getElementById('address').value,
            bio: document.getElementById('bio').value,
            skills: [],
            education: [],
            experience: []
        };
        
        // Collect existing items from UI, not from form fields
        const skillItems = document.querySelectorAll('.skill-item span');
        const educationItems = document.querySelectorAll('.education-item span');
        const experienceItems = document.querySelectorAll('.experience-item span');
        
        // Convert to arrays of text content
        skillItems.forEach(item => formData.skills.push(item.textContent));
        educationItems.forEach(item => formData.education.push(item.textContent));
        experienceItems.forEach(item => formData.experience.push(item.textContent));
        
        // Add the new value if it doesn't already exist
        if (type === 'skills' && newValue && !formData.skills.includes(newValue)) {
            formData.skills.push(newValue);
        } else if (type === 'education' && newValue && !formData.education.includes(newValue)) {
            formData.education.push(newValue);
        } else if (type === 'experience' && newValue && !formData.experience.includes(newValue)) {
            formData.experience.push(newValue);
        }
        
        // Make sure to use Sets to eliminate duplicates
        formData.skills = Array.from(new Set(formData.skills));
        formData.education = Array.from(new Set(formData.education));
        formData.experience = Array.from(new Set(formData.experience));
        
        console.log('Submitting complete profile data:', formData);
        
        // Clear input field for the type that was just added
        const inputFields = document.querySelectorAll(`input[type="text"][placeholder*="${type === 'skills' ? 'compétence' : type === 'education' ? 'formation' : 'expérience'}"]`);
        inputFields.forEach(field => {
            field.value = '';
        });
        
        // Submit the data
        submitProfileData(formData, `${type === 'skills' ? 'Compétence' : type === 'education' ? 'Formation' : 'Expérience'} ajoutée avec succès`);
    }
    
    // Function to display recruiter profile
    function displayRecruiterProfile(user, company) {
        const profileContent = document.getElementById('profile-content');
        const template = document.getElementById('recruiter-profile-template');
        
        // Clone the template
        const profileElement = template.content.cloneNode(true);
        profileContent.innerHTML = '';
        profileContent.appendChild(profileElement);
        
        // Set profile header information
        document.getElementById('profile-name').textContent = `${user.firstName} ${user.lastName}`;
        document.getElementById('profile-email').textContent = user.email;
        
        // Set profile picture
        const profilePictureContainer = document.getElementById('profile-picture-container');
        if (user.profilePicturePath) {
            // Add a timestamp to the image URL to prevent caching
            const imageUrl = `/uploads/${user.profilePicturePath}?t=${new Date().getTime()}`;
            console.log('Profile picture path:', user.profilePicturePath);
            console.log('Full image URL:', imageUrl);
            profilePictureContainer.innerHTML = `
                <img src="${imageUrl}" class="profile-picture" alt="Profile picture" onerror="console.error('Failed to load image:', this.src)">
            `;
        } else {
            profilePictureContainer.innerHTML = `
                <div class="profile-picture-placeholder">
                    <i class="bi bi-person-circle"></i>
                </div>
            `;
        }
        
        // Fill form fields
        document.getElementById('firstName').value = user.firstName || '';
        document.getElementById('lastName').value = user.lastName || '';
        document.getElementById('email').value = user.email || '';
        document.getElementById('phone').value = user.phone || '';
        document.getElementById('position').value = user.position || '';
        
        // Fill company information if available
        if (company) {
            document.getElementById('companyId').value = company.id || '';
            document.getElementById('companyName').value = company.name || '';
            document.getElementById('companyIndustry').value = company.industry || '';
            document.getElementById('companyLocation').value = company.location || '';
            document.getElementById('companyWebsite').value = company.website || '';
            document.getElementById('companyDescription').value = company.description || '';
            
            // Show company logo if available
            if (company.logoPath) {
                document.getElementById('company-logo-preview').innerHTML = `
                    <p><strong>Logo actuel:</strong></p>
                    <div class="d-flex align-items-center">
                        <img src="/uploads/${company.logoPath}" alt="Company logo" style="max-width: 200px; max-height: 100px; margin-right: 15px;">
                        <button type="button" id="delete-company-logo-btn" class="btn btn-sm btn-danger">
                             <i class="bi bi-trash"></i> Supprimer le logo
                        </button>
                    </div>
                `;
                document.getElementById('company-logo-preview').style.display = 'block';
            }
        }
        
        // Set up event listeners for form submissions
        setupRecruiterFormSubmission();
        
        // Set up event listeners for file uploads
        setupFileUploads();
        
        // Set up event listener for deleting company logo
        setupDeleteCompanyLogoButton();
    }
    
    // Helper function to get filename from path
    function getFilenameFromPath(path) {
        return path.split('/').pop();
    }
    
    // Function to set up event listeners for candidate form submission
    function setupCandidateFormSubmission() {
        const form = document.getElementById('candidate-profile-form');
        
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            
            const saveButton = document.getElementById('save-profile-btn');
            const errorMessage = document.getElementById('profile-error');
            const successMessage = document.getElementById('profile-success');
            
            // Disable button and show loading state
            saveButton.disabled = true;
            saveButton.innerHTML = '<div class="loading-spinner"></div> Enregistrement...';
            errorMessage.style.display = 'none';
            successMessage.style.display = 'none';
            
            // Get all input fields for skills, education, and experience
            const skillInputs = document.querySelectorAll('input[placeholder*="compétence"]');
            const educationInputs = document.querySelectorAll('input[placeholder*="formation"]');
            const experienceInputs = document.querySelectorAll('input[placeholder*="expérience"]');
            
            // Collect skills, education, and experience values
            const skills = Array.from(new Set(Array.from(skillInputs)
                .map(input => input.value.trim())
                .filter(value => value)));
                
            const education = Array.from(new Set(Array.from(educationInputs)
                .map(input => input.value.trim())
                .filter(value => value)));
                
            const experience = Array.from(new Set(Array.from(experienceInputs)
                .map(input => input.value.trim())
                .filter(value => value)));
            
            // Gather form data - explicitly create object with expected structure
            const formData = {
                firstName: document.getElementById('firstName').value,
                lastName: document.getElementById('lastName').value,
                phone: document.getElementById('phone').value,
                address: document.getElementById('address').value,
                bio: document.getElementById('bio').value,
                skills: skills,
                education: education,
                experience: experience
            };
            
            console.log('Sending form data:', formData);
            
            // Send the data
            fetch('/api/profile/candidate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token
                },
                body: JSON.stringify(formData)
            })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => {
                        throw new Error(err.message || 'Failed to update profile');
                    });
                }
                return response.json();
            })
            .then(data => {
                console.log('Update response:', data);
                
                // Show success message
                successMessage.textContent = data.message || 'Profile updated successfully';
                successMessage.style.display = 'block';
                
                // Reset button state
                saveButton.disabled = false;
                saveButton.innerHTML = '<i class="bi bi-save"></i> Enregistrer';
                
                // Reload profile after 2 seconds to see updated data
                setTimeout(loadUserProfile, 2000);
            })
            .catch(error => {
                console.error('Error updating profile:', error);
                
                // Show error message
                errorMessage.textContent = error.message || 'Failed to update profile';
                errorMessage.style.display = 'block';
                
                // Reset button state
                saveButton.disabled = false;
                saveButton.innerHTML = '<i class="bi bi-save"></i> Enregistrer';
            });
        });
    }
    
    // Function to set up event listeners for recruiter form submission
    function setupRecruiterFormSubmission() {
        const profileForm = document.getElementById('recruiter-profile-form');
        
        // Add a single submit listener to the profile form (or the main container if appropriate)
        profileForm.addEventListener('submit', function(event) {
            event.preventDefault();
            
            // Identify which button was clicked to show the correct loading/success message
            const submitButton = event.submitter; // Get the button that triggered the submit
            const isCompanySubmit = submitButton && submitButton.id === 'save-company-btn';
            
            const saveButton = isCompanySubmit ? document.getElementById('save-company-btn') : document.getElementById('save-profile-btn');
            const errorMessageElement = isCompanySubmit ? document.getElementById('company-error') : document.getElementById('profile-error');
            const successMessageElement = isCompanySubmit ? document.getElementById('company-success') : document.getElementById('profile-success');
            
            // Get profile picture file input
            const profilePictureInput = document.getElementById('profile-picture-upload');
            const profilePictureFile = profilePictureInput && profilePictureInput.files.length > 0 ? profilePictureInput.files[0] : null;

            // Get company logo file input
            const companyLogoInput = document.getElementById('company-logo-upload');
            const companyLogoFile = companyLogoInput && companyLogoInput.files.length > 0 ? companyLogoInput.files[0] : null;
            
            // Create FormData object
            const formData = new FormData();

            // Append personal text fields (from profileForm)
            formData.append('firstName', document.getElementById('firstName').value);
            formData.append('lastName', document.getElementById('lastName').value);
            formData.append('phone', document.getElementById('phone').value);
            formData.append('position', document.getElementById('position').value);
            
            // Append company text fields (from profileForm)
            const companyIdField = document.getElementById('companyId');
            if (companyIdField && companyIdField.value) { // Only append if companyId exists and has a value
                formData.append('companyId', companyIdField.value);
            }
            formData.append('companyName', document.getElementById('companyName').value);
            formData.append('companyIndustry', document.getElementById('companyIndustry').value);
            formData.append('companyLocation', document.getElementById('companyLocation').value);
            formData.append('companyWebsite', document.getElementById('companyWebsite').value);
            formData.append('companyDescription', document.getElementById('companyDescription').value);

            // Append profile picture file if selected
            if (profilePictureFile) {
                formData.append('profilePicture', profilePictureFile);
            }
            
            // Append company logo file if selected
            if (companyLogoFile) {
                 // Ensure companyId is present before appending logo file
                 if (!companyIdField || !companyIdField.value) {
                      errorMessageElement.textContent = 'Please save company information first before uploading a logo.';
                      errorMessageElement.style.display = 'block';
                      return; // Stop submission if no companyId for logo upload
                 }
                 formData.append('companyLogo', companyLogoFile); // Use 'companyLogo' as the parameter name
            }
            
            // Disable button and show loading state
            if (saveButton) {
            saveButton.disabled = true;
            saveButton.innerHTML = '<div class="loading-spinner"></div> Enregistrement...';
            }
            if (errorMessageElement) errorMessageElement.style.display = 'none';
            if (successMessageElement) successMessageElement.style.display = 'none';
            
            // Send the data
            fetch('/api/profile/recruiter', {
                method: 'POST',
                // Content-Type header is NOT needed for FormData, browser sets it correctly
                headers: {
                    'Authorization': 'Bearer ' + token
                },
                body: formData
            })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => {
                        throw new Error(err.message || (isCompanySubmit ? 'Failed to update company information' : 'Failed to update profile'));
                    });
                }
                return response.json();
            })
            .then(data => {
                // Show success message
                if (successMessageElement) {
                    successMessageElement.textContent = data.message || (isCompanySubmit ? 'Company information updated successfully' : 'Profile updated successfully');
                    successMessageElement.style.display = 'block';
                }
                
                // Re-enable button
                if (saveButton) {
                saveButton.disabled = false;
                    saveButton.innerHTML = isCompanySubmit ? '<i class="bi bi-save"></i> Enregistrer les informations de l\'entreprise' : '<i class="bi bi-save"></i> Enregistrer';
                }
                
                // Update company ID if needed
                if (data.company && data.company.id) {
                    document.getElementById('companyId').value = data.company.id;
                }
                
                // Reload profile after 2 seconds
                setTimeout(loadUserProfile, 2000);
            })
            .catch(error => {
                // Show error message
                if (errorMessageElement) {
                    errorMessageElement.textContent = error.message || (isCompanySubmit ? 'Failed to update company information' : 'Failed to update profile');
                    errorMessageElement.style.display = 'block';
                }
                
                // Re-enable button
                if (saveButton) {
                saveButton.disabled = false;
                    saveButton.innerHTML = isCompanySubmit ? '<i class="bi bi-save"></i> Enregistrer les informations de l\'entreprise' : '<i class="bi bi-save"></i> Enregistrer';
                }
            });
        });
    }
    
    // Function to set up event listeners for file uploads
    function setupFileUploads() {
        // Profile picture upload (for candidate)
        const profilePictureUpload = document.getElementById('profile-picture-upload');
        if (profilePictureUpload) {
            profilePictureUpload.addEventListener('change', function(event) {
                if (event.target.files.length === 0) return;
                const file = event.target.files[0];
                const preview = document.getElementById('profile-picture-preview');
                const progress = document.getElementById('profile-picture-progress');
                preview.innerHTML = `<p><strong>Fichier sélectionné :</strong> ${file.name}</p>`;
                preview.style.display = 'block';

                // --- ACTUAL UPLOAD ---
                const formData = new FormData();
                formData.append('file', file);
                fetch('/api/profile/candidate/profile-picture', {
                    method: 'POST',
                    headers: { 'Authorization': 'Bearer ' + token },
                    body: formData
                })
                .then(response => response.json())
                .then(data => {
                    // Optionally show a message
                    // Reload profile to show new picture
                    loadUserProfile();
                })
                .catch(err => {
                    preview.innerHTML += '<div class="text-danger">Erreur lors de l\'upload</div>';
                });
            });
        }

        // CV upload (for candidate)
        const cvUpload = document.getElementById('cv-upload');
        if (cvUpload) {
            cvUpload.addEventListener('change', function(event) {
                if (event.target.files.length === 0) return;
                const file = event.target.files[0];
                const preview = document.getElementById('cv-preview');
                const progress = document.getElementById('cv-progress');
                preview.innerHTML = `<p><strong>Fichier sélectionné :</strong> ${file.name}</p>`;
                preview.style.display = 'block';

                // --- ACTUAL UPLOAD ---
                const formData = new FormData();
                formData.append('file', file);
                fetch('/api/profile/candidate/cv', {
                    method: 'POST',
                    headers: { 'Authorization': 'Bearer ' + token },
                    body: formData
                })
                .then(response => response.json())
                .then(data => {
                    // Optionally show a message
                    // Reload profile to show new CV
                    loadUserProfile();
                })
                .catch(err => {
                    preview.innerHTML += '<div class="text-danger">Erreur lors de l\'upload</div>';
                });
            });
        }

        // Company Logo upload preview
        const companyLogoUpload = document.getElementById('company-logo-upload');
        if (companyLogoUpload) {
            companyLogoUpload.addEventListener('change', function(event) {
                if (event.target.files.length === 0) return;
                const file = event.target.files[0];
                const preview = document.getElementById('company-logo-preview');
                preview.innerHTML = `<p><strong>Fichier sélectionné :</strong> ${file.name}</p>`;
                preview.style.display = 'block';
                // Clear any previous success/error messages for logo upload
                const errorMessageElement = document.getElementById('company-error');
                const successMessageElement = document.getElementById('company-success');
                if (errorMessageElement) errorMessageElement.style.display = 'none';
                if (successMessageElement) successMessageElement.style.display = 'none';
                // Note: Actual upload happens on form submission (when save button is clicked)
            });
        }
    }
    
    // Function to set up event listener for deleting company logo
    function setupDeleteCompanyLogoButton() {
         const deleteButton = document.getElementById('delete-company-logo-btn');
         if (deleteButton) {
             deleteButton.addEventListener('click', function() {
                 const companyIdField = document.getElementById('companyId');
                 const companyId = companyIdField ? companyIdField.value : null;
                
                if (!companyId) {
                      console.error('Company ID not found for logo deletion.');
                      // Optionally show an error message to the user
                    return;
                }
                
                 if (!confirm('Êtes-vous sûr de vouloir supprimer le logo de l\'entreprise ?')) {
                      return;
                 }
                 
                 // Disable button and show loading state
                 deleteButton.disabled = true;
                 deleteButton.innerHTML = '<div class="loading-spinner"></div> Suppression...';
                 
                 const token = auth.getToken(); // Assuming auth is available
                
                 fetch(`/api/profile/company/logo/${companyId}`, {
                     method: 'DELETE',
                    headers: {
                        'Authorization': 'Bearer ' + token
                     }
                })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(err => {
                             throw new Error(err.message || 'Failed to delete company logo');
                        });
                    }
                    return response.json();
                })
                .then(data => {
                     console.log('Delete logo response:', data);
                     // Show success message (using company success message element)
                     const successMessageElement = document.getElementById('company-success');
                     if (successMessageElement) {
                         successMessageElement.textContent = data.message || 'Company logo deleted successfully';
                         successMessageElement.style.display = 'block';
                     }
                    
                     // Reload profile after a short delay
                     setTimeout(loadUserProfile, 1000);
                })
                .catch(error => {
                     console.error('Error deleting company logo:', error);
                     // Show error message (using company error message element)
                      const errorMessageElement = document.getElementById('company-error');
                     if (errorMessageElement) {
                         errorMessageElement.textContent = error.message || 'Failed to delete company logo';
                         errorMessageElement.style.display = 'block';
                     }
                     
                     // Re-enable button
                     deleteButton.disabled = false;
                      deleteButton.innerHTML = '<i class="bi bi-trash"></i> Supprimer le logo';
                });
            });
        }
    }
}); 