package org.example.backend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.example.backend.common.ErrorCode;
import org.example.backend.constant.CommonConstant;
import org.example.backend.exception.BusinessException;
import org.example.backend.exception.ThrowUtils;
import org.example.backend.mapper.QuestionBankQuestionMapper;
import org.example.backend.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import org.example.backend.model.entity.Question;
import org.example.backend.model.entity.QuestionBankQuestion;
import org.example.backend.model.entity.QuestionnaireBank;
import org.example.backend.model.entity.User;
import org.example.backend.model.vo.QuestionBankQuestionVO;
import org.example.backend.model.vo.QuestionVO;
import org.example.backend.model.vo.QuestionnaireBankVO;
import org.example.backend.model.vo.UserVO;
import org.example.backend.service.QuestionBankQuestionService;
import org.example.backend.service.QuestionService;
import org.example.backend.service.QuestionnaireBankService;
import org.example.backend.service.UserService;
import org.example.backend.utils.SqlUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关系表服务实现
 */
@Service
@Slf4j
public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {

    @Resource
    private UserService userService;


    @Resource
    @Lazy
    private QuestionService questionService;

    @Resource
    @Lazy
    private QuestionnaireBankService questionnaireBankService;

    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.PARAMS_ERROR);
        Long questionId = questionBankQuestion.getQuestionId();
        if(questionId != null) {
            Question question = questionService.getById(questionId);
            ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR, "题目不存在");
        }

        Long questionBankId = questionBankQuestion.getQuestionBankId();
        if(questionBankId != null) {
            QuestionnaireBank questionnaire = questionnaireBankService.getById(questionBankId);
            ThrowUtils.throwIf(questionnaire == null, ErrorCode.PARAMS_ERROR, "题目库不存在");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest) {
        QueryWrapper<QuestionBankQuestion> queryWrapper = new QueryWrapper<>();
        if (questionBankQuestionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionBankQuestionQueryRequest.getId();
        Long notId = questionBankQuestionQueryRequest.getNotId();
        Long questionId = questionBankQuestionQueryRequest.getQuestionId();
        Long questionBankId = questionBankQuestionQueryRequest.getQuestionBankId();
        String sortField = questionBankQuestionQueryRequest.getSortField();
        String sortOrder = questionBankQuestionQueryRequest.getSortOrder();
        Long userId = questionBankQuestionQueryRequest.getUserId();

        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionBankId), "questionBankId", questionBankId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取关系表封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    @Override
    public QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request) {
        // 对象转封装类
        QuestionBankQuestionVO questionBankQuestionVO = QuestionBankQuestionVO.objToVo(questionBankQuestion);

        // region 可选
        // 1. 关联查询用户信息
        Long userId = questionBankQuestion.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        // 2. 关联题目id
        Long questionId = questionBankQuestion.getQuestionId();
        Question question = null;
        if(questionId != 0 && questionId > 0) {
            question = questionService.getById(questionId);
        }
        QuestionVO questionVO = questionService.getQuestionVO(question, request);
        //3. 关联问卷id
        Long questionBankId = questionBankQuestion.getQuestionBankId();
        QuestionnaireBank questionnaireBank = null;
        if(questionBankId != 0 && questionBankId > 0) {
            questionnaireBank = questionnaireBankService.getById(questionBankId);
        }
        QuestionnaireBankVO questionnaireBankVO = questionnaireBankService.getQuestionnaireBankVO(questionnaireBank, request);

        questionBankQuestionVO.setUserVO(userVO);
        questionBankQuestionVO.setQuestionVO(questionVO);
        questionBankQuestionVO.setQuestionnaireBankVO(questionnaireBankVO);
        return questionBankQuestionVO;
    }

    /**
     * 分页获取关系表封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request) {
        List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionPage.getRecords();
        Page<QuestionBankQuestionVO> questionBankQuestionVOPage = new Page<>(questionBankQuestionPage.getCurrent(), questionBankQuestionPage.getSize(), questionBankQuestionPage.getTotal());
        if (CollUtil.isEmpty(questionBankQuestionList)) {
            return questionBankQuestionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBankQuestionVO> questionBankQuestionVOList = questionBankQuestionList.stream().map(questionBankQuestion -> {
            return QuestionBankQuestionVO.objToVo(questionBankQuestion);
        }).collect(Collectors.toList());

        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionBankQuestionList.stream().map(QuestionBankQuestion::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        questionBankQuestionVOList.forEach(questionBankQuestionVO -> {
            Long userId = questionBankQuestionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBankQuestionVO.setUserVO(userService.getUserVO(user));
        });
        // endregion

        questionBankQuestionVOPage.setRecords(questionBankQuestionVOList);
        return questionBankQuestionVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddQuestionsToBank(List<Long> questionIdList, Long questionBankId, User loginUser) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(questionBankId == null || questionBankId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);

        List<Question> questionList = questionService.listByIds(questionIdList);
        // 合法的题目Id
        List<Long> validQuestionIdList = questionList.stream().map(Question::getId).collect(Collectors.toList());
        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList),ErrorCode.PARAMS_ERROR);
        // 检查题库id
        QuestionnaireBank questionBank = questionnaireBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.PARAMS_ERROR);

        // 检查题目是否已经在题库中了
        LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .in(QuestionBankQuestion::getQuestionId, validQuestionIdList);
        List<QuestionBankQuestion> existQuestionList = this.list(lambdaQueryWrapper);
        Set<Long> existQuestionIdSet = existQuestionList.stream()
                .map(QuestionBankQuestion::getQuestionId)
                .collect(Collectors.toSet());
        validQuestionIdList = validQuestionIdList.stream().filter(questionId -> {
            return !existQuestionIdSet.contains(questionId);
        }).collect(Collectors.toList());
        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList), ErrorCode.PARAMS_ERROR, "所有题目已经存在");
        //执行插入
        for(Long questionId : validQuestionIdList) {
            QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
            questionBankQuestion.setQuestionBankId(questionBankId);
            questionBankQuestion.setQuestionId(questionId);
            questionBankQuestion.setUserId(loginUser.getId());
            boolean result = this.save(questionBankQuestion);
            if(!result) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加题目失败");
            }
        }
    }

    @Override
    public void batchRemoveQuestionsFromBank(List<Long> questionIdList, Long questionBankId) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(questionBankId == null || questionBankId <= 0, ErrorCode.PARAMS_ERROR);
        for (Long questionId : questionIdList) {
            //构造查询
            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId, questionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            boolean result = this.remove(lambdaQueryWrapper);
            if(!result) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "移除题目失败");
            }
        }
    }

}